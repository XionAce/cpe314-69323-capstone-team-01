"""Export the two Markdown masters to PDF. Requires reportlab and Calibri fonts.

Run: python docs/export_documents.py
Set CALIBRI_DIR if Calibri is not in C:/Windows/Fonts.
The renderer reads document text, tables, SQL and ERD fields from Markdown only.
"""
from pathlib import Path
import os
import re
from html import escape
from xml.etree import ElementTree as ET
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.lib import colors
from reportlab.lib.styles import ParagraphStyle
from reportlab.platypus import (BaseDocTemplate, PageTemplate, Frame, Paragraph,
                               Spacer, Table, TableStyle, PageBreak, Preformatted, KeepTogether, HRFlowable)
from reportlab.platypus.tableofcontents import TableOfContents
from reportlab.graphics.shapes import Drawing, Rect, String, Line

ROOT = Path(__file__).resolve().parent
FONT = Path(os.environ.get('CALIBRI_DIR', 'C:/Windows/Fonts'))
for name, file in [('Calibri', 'calibri.ttf'), ('Calibri-Bold', 'calibrib.ttf'),
                   ('Calibri-Italic', 'calibrii.ttf')]:
    pdfmetrics.registerFont(TTFont(name, str(FONT / file)))
pdfmetrics.registerFontFamily('Calibri', normal='Calibri', bold='Calibri-Bold', italic='Calibri-Italic')
NAVY = colors.HexColor('#1d2d50')
GOLD = colors.HexColor('#c89032')
styles = {
    'body': ParagraphStyle('body', fontName='Calibri', fontSize=11, leading=13.2, spaceAfter=7),
    'title': ParagraphStyle('title', fontName='Calibri-Bold', fontSize=20, leading=24, textColor=NAVY, spaceAfter=14),
    'h1': ParagraphStyle('h1', fontName='Calibri-Bold', fontSize=18, leading=21, textColor=NAVY, spaceAfter=10, keepWithNext=True),
    'h2': ParagraphStyle('h2', fontName='Calibri-Bold', fontSize=12, leading=15, textColor=NAVY, spaceBefore=8, spaceAfter=7, keepWithNext=True),
    'cell': ParagraphStyle('cell', fontName='Calibri', fontSize=11, leading=11.5),
    'headcell': ParagraphStyle('headcell', fontName='Calibri-Bold', fontSize=11, leading=11.5, textColor=colors.white),
    'caption': ParagraphStyle('caption', fontName='Calibri', fontSize=11, leading=13, spaceBefore=6, spaceAfter=6),
    'code': ParagraphStyle('code', fontName='Courier', fontSize=9, leading=11, spaceAfter=8),
}

def inline(text):
    text = escape(text)
    text = re.sub(r'\[([^]]+)\]\(([^)]+)\)', r'<link href="\2" color="#245a85">\1</link>', text)
    text = re.sub(r'\*\*(.+?)\*\*', r'<b>\1</b>', text)
    text = re.sub(r'`([^`]+)`', r'<font name="Courier" size="9">\1</font>', text)
    return text

def p(text, kind='body'):
    return Paragraph(inline(text), styles[kind])

def drawio_sketch(filename):
    """Paint the editable sketch's table rows and field-to-field connectors."""
    graph = ET.parse(ROOT / filename).find('.//mxGraphModel')
    height = int(graph.get('pageHeight'))
    drawing = Drawing(int(graph.get('pageWidth')), height)
    cells = {cell.get('id'): cell for cell in graph.findall('root/mxCell')}
    bounds = {'1': (0, 0, 0, 0)}
    line_color = colors.HexColor('#456281')
    for cell in cells.values():
        if cell.get('vertex') != '1':
            continue
        geom = cell.find('mxGeometry')
        px, py, _, _ = bounds[cell.get('parent')]
        x, y, w, h = (float(geom.get(k)) for k in ('x','y','width','height'))
        bounds[cell.get('id')] = (px+x, py+y, w, h)
    def line(a,b,color=line_color,width=1.1):
        drawing.add(Line(a[0],height-a[1],b[0],height-b[1],strokeColor=color,strokeWidth=width))
    for edge in (c for c in cells.values() if c.get('edge') == '1'):
        style = dict(re.findall(r'(\w+)=([^;]+)', edge.get('style')))
        def anchor(key, side):
            x,y,w,h = bounds[edge.get(key)]
            return (x+w*float(style[side]), y+h/2)
        start, end = anchor('source','exitX'), anchor('target','entryX')
        pts = [start]+[(float(p.get('x')),float(p.get('y'))) for p in edge.findall('.//mxPoint')]+[end]
        for a,b in zip(pts,pts[1:]):
            line(a,b)
        # Single bar at the parent and a crow's foot at the foreign key.
        x,y = start
        bar = x + (6 if pts[1][0]>x else -6)
        line((bar,y-3.4),(bar,y+3.4))
        x,y = end
        fork = x + (8 if pts[-2][0]>x else -8)
        for delta in (-3.6,3.6):
            line((fork,y),(x,y+delta))
    for id,cell in cells.items():
        if cell.get('vertex') != '1' or cell.get('style')=='group;':
            continue
        x,y,w,h = bounds[id]
        style = dict(re.findall(r'(\w+)=([^;]+)', cell.get('style')))
        if style.get('strokeColor') != 'none':
            drawing.add(Rect(x,height-y-h,w,h,fillColor=colors.HexColor(style['fillColor']),
                strokeColor=colors.HexColor(style['strokeColor']),strokeWidth=float(style.get('strokeWidth',1))))
        size = float(style['fontSize'])
        font = 'Calibri-Bold' if style.get('fontStyle')=='1' else 'Calibri'
        left = style.get('align')=='left'
        assert pdfmetrics.stringWidth(cell.get('value'),font,size) < w-(6 if left else 0), id
        drawing.add(String(x+4 if left else x+w/2,height-y-h/2-size*.31,cell.get('value'),
                           textAnchor='start' if left else 'middle',fontName=font,fontSize=size,
                           fillColor=colors.HexColor(style.get('fontColor','#19283D'))))
    return drawing

def erd(source):
    """Use the same entities/attributes/relations as the Markdown Mermaid block."""
    nodes = {}
    for name, body in re.findall(r'(\w+)\s*\{([^}]+)\}', source):
        nodes[name] = [' '.join(line.split()) for line in body.splitlines() if line.strip()]
    if 'Members' in nodes:
        positions = {'Members': (20, 435), 'Sellers': (266, 435), 'Listings': (266, 260),
                     'Reviews': (20, 260), 'BuyerInterests': (20, 100)}
        height = 450
        routes = {
            ('Members','Sellers'): [(210,403),(266,403)],
            ('Members','Reviews'): [(85,373),(85,260)],
            ('Members','BuyerInterests'): [(20,389),(4,389),(4,125),(40,125),(40,100)],
            ('Sellers','Listings'): [(361,345),(361,260)],
            ('Listings','Reviews'): [(266,205),(210,205)],
            ('Listings','BuyerInterests'): [(361,114),(361,57),(210,57)],
        }
    elif 'HistoricalSales' in nodes:
        positions = {'Categories': (0,435), 'Listings': (266,260), 'HistoricalSales': (0,160)}
        height = 450
        routes = {
            ('Categories','Categories'): [(190,408),(222,408),(222,370),(190,370)],
            ('Categories','Listings'): [(95,359),(95,295),(361,295),(361,260)],
            ('Categories','HistoricalSales'): [(65,359),(65,160)],
        }
    else:
        positions = {'Listings': (133,465), 'TextbookDetails': (0,250),
                     'ElectronicsDetails': (266,250), 'FurnitureDetails': (133,90)}
        height = 480
        routes = {
            ('Listings','TextbookDetails'): [(180,319),(180,280),(95,280),(95,250)],
            ('Listings','ElectronicsDetails'): [(276,319),(276,280),(361,280),(361,250)],
            ('Listings','FurnitureDetails'): [(228,319),(228,90)],
        }
    d = Drawing(468, height)
    for left, lc, rc, right, label in re.findall(r'(\w+)\s+(\|\||o\|)--(o\{|o\||\|\|)\s+(\w+)\s*:\s*(\w+)', source):
        pts = routes[(left,right)]
        for a,b in zip(pts,pts[1:]):
            d.add(Line(*a,*b,strokeColor=colors.HexColor('#5c759b'),strokeWidth=0.8))
        # Place cardinalities in the gap outside each box, not over its fields.
        ax,ay=pts[0]; bx,by=pts[1]; dx=(bx>ax)-(bx<ax); dy=(by>ay)-(by<ay)
        d.add(String(ax+dx*12+(3 if dx>=0 else -5),ay+dy*12+4,
                     '0..1' if lc=='o|' else '1',fontName='Calibri',fontSize=9))
        ax,ay=pts[-2]; bx,by=pts[-1]; dx=(bx>ax)-(bx<ax); dy=(by>ay)-(by<ay)
        d.add(String(bx+(-30 if dx>0 else 12 if dx<0 else 3),by-dy*12+4,
                     {'o{':'0..*','o|':'0..1','||':'1'}[rc],fontName='Calibri',fontSize=9))
    for name, fields in nodes.items():
        x,top = positions[name]
        h = 34 + len(fields)*14
        d.add(Rect(x,top-h,190,h,fillColor=colors.white,strokeColor=colors.HexColor('#8fa2bf'),strokeWidth=0.7))
        d.add(Rect(x,top-21,190,21,fillColor=NAVY,strokeColor=NAVY))
        d.add(String(x+5,top-15,name,fontName='Calibri-Bold',fontSize=11,fillColor=colors.white))
        for i,field in enumerate(fields):
            d.add(String(x+5,top-36-i*14,field,fontName='Calibri',fontSize=10,fillColor=NAVY))
    return d

def design_erd(source):
    """Connect schematic keys to the exact row centres in each database view."""
    fields = {}
    for table, body in re.findall(r'(\w+)\s*\{([^}]+)\}',source):
        fields[table] = [re.fullmatch(r'(\w+) (\w+)(?: ([A-Z,]+))?(?: "([^"]+)")?',s.strip()).groups()
                         for s in body.splitlines() if s.strip()]
    if 'Members' in fields:
        xy = {'Members':(31,6),'Sellers':(261,6),'Reviews':(31,156),'Listings':(261,156),'BuyerInterests':(31,310)}
        links = [('Members','Sellers','memberId',1,0,232),('Members','Reviews','reviewerId',0,0,17),
                 ('Members','BuyerInterests','buyerId',0,0,4),('Sellers','Listings','sellerId',1,1,457),
                 ('Listings','Reviews','listingId',0,1,224),('Listings','BuyerInterests','listingId',0,1,248)]
    elif 'Categories' in fields:
        xy = {'Categories':(32,10),'Listings':(265,25),'HistoricalSales':(265,236)}
        links = [('Categories','Categories','parentId',0,0,10),('Categories','Listings','categoryId',1,0,245),
                 ('Categories','HistoricalSales','categoryId',1,0,220)]
    else:
        xy = {'Listings':(7,147),'TextbookDetails':(264,8),'ElectronicsDetails':(264,158),'FurnitureDetails':(264,304)}
        links = [('Listings','TextbookDetails','listingId',1,0,218),('Listings','ElectronicsDetails','listingId',1,0,236),
                 ('Listings','FurnitureDetails','listingId',1,0,208)]
    w,head,row = 180,22,18
    h = max(top+head+row*len(fields[n]) for n,(_,top) in xy.items())+5
    d=Drawing(468,h)
    ink=colors.HexColor('#456281')
    def line(x1,y1,x2,y2,color=ink,weight=1):
        d.add(Line(x1,h-y1,x2,h-y2,strokeColor=color,strokeWidth=weight))
    def txt(x,y,value,size=10,bold=False,anchor='start',color=NAVY):
        d.add(String(x,h-y,value,fontName='Calibri-Bold' if bold else 'Calibri',fontSize=size,textAnchor=anchor,fillColor=color))
    def port(table,key,side):
        index=next(i for i,f in enumerate(fields[table]) if f[1]==key)
        x,y=xy[table]
        return x+side*w,y+head+(index+.5)*row
    for parent,child,fk,side,entry,lane in links:
        sx,sy=port(parent,'id',side); ex,ey=port(child,fk,entry)
        line(sx,sy,lane,sy);line(lane,sy,lane,ey);line(lane,ey,ex,ey)
        px=sx+(6 if side else -6); line(px,sy-3.5,px,sy+3.5)
        fork=ex+(8 if entry else -8)
        cardinality='1:1' if child.endswith('Details') else '1:M'
        if cardinality=='1:M':
            line(fork,ey,ex,ey-4);line(fork,ey,ex,ey+4)
        else:
            line(fork,ey-3.5,fork,ey+3.5)
        if 'Members' in fields and fk in ('reviewerId','buyerId','sellerId'):
            txt(lane+4 if not side else lane-4,290 if fk=="buyerId" else 128,cardinality,10,True,'start' if not side else 'end')
        elif parent=='Listings' and child=='Reviews':
            txt(lane+3,ey-3,cardinality,9,True)
        elif parent==child:
            txt(lane+2,ey+13,cardinality,9,True)
        elif side==entry:
            txt(lane+3 if side==0 else lane-3,ey-5,cardinality,9,True,'start' if side==0 else 'end')
        else:
            txt((ex+lane)/2,ey-5,cardinality,9,True,'middle')
    for table,(x,y) in xy.items():
        d.add(Rect(x,h-y-head,w,head,fillColor=NAVY,strokeColor=NAVY))
        txt(x+w/2,y+15,table,12,True,'middle',colors.white)
        for i,(typ,key,flags,label) in enumerate(fields[table]):
            top=y+head+i*row
            d.add(Rect(x,h-top-row,w,row,fillColor=colors.white,strokeColor=NAVY,strokeWidth=.55))
            line(x+31,top,x+31,top+row,NAVY,.5)
            txt(x+3,top+12,flags or '',8,True)
            txt(x+35,top+12,label or key,9.5)
            txt(x+w-3,top+12,typ,8,False,'end')
    return d

class Document(BaseDocTemplate):
    def __init__(self, target, title):
        super().__init__(str(target), pagesize=(612,792), leftMargin=72, rightMargin=72,
                         topMargin=72, bottomMargin=72, title=title, author='Group 1')
        self.addPageTemplates(PageTemplate(id='main', frames=[Frame(72,72,468,648,
            leftPadding=0,rightPadding=0,topPadding=0,bottomPadding=0)], onPage=self.footer))
    def footer(self, canvas, doc):
        design = Path(self.filename).stem=='design-document'
        if design and doc.page<=3:
            return
        number=doc.page-3 if design else doc.page
        canvas.setFont('Calibri',8)
        canvas.setFillColor(NAVY)
        canvas.drawCentredString(306,42,f'CPE 314 | Software Design   ·   University of Cebu   ·   Richyll Son   ·   Page {number}')
    def afterFlowable(self, item):
        if isinstance(item,Paragraph) and getattr(item,'toc',False):
            page=self.page
            if Path(self.filename).stem=='design-document':
                if page<=3: return
                page-=3
            self.notify('TOCEntry',(0 if item.style.name=='h1' else 1,item.getPlainText(),page))

def export(name):
    lines=(ROOT/f'{name}.md').read_text(encoding='utf-8').splitlines()
    story=[]; i=0
    center=lambda t,size,font,color: Paragraph(escape(t),ParagraphStyle('center',fontName=font,fontSize=size,leading=size*1.25,alignment=1,textColor=color))
    if name=='design-document':
        head=[line.strip().removeprefix('# ') for line in lines[:lines.index('Table 1: Group information')] if line.strip()]
        i=lines.index('Table 1: Group information')
        story.append(Spacer(1,76))
        for t in head[:3]: story.extend([center(t,12,'Calibri',NAVY),Spacer(1,9)])
        story.extend([Spacer(1,60),center(head[3],24,'Calibri-Bold',NAVY),Spacer(1,12),center(head[4],14,'Calibri-Italic',GOLD),Spacer(1,65)])
    while i<len(lines):
        line=lines[i].strip(); i+=1
        if not line: continue
        if line=='<!-- pagebreak -->':
            while story and isinstance(story[-1], Spacer):
                story.pop()
            story.append(PageBreak()); continue
        if line=='<!-- toc -->':
            toc=TableOfContents(); toc.levelStyles=[ParagraphStyle('toc0',parent=styles['body'],spaceBefore=3),
                ParagraphStyle('toc1',parent=styles['body'],leftIndent=14)]
            story.append(toc); continue
        if line.startswith('<!--') or line.startswith('[Editable diagram]'): continue
        if line.startswith('![') and line.endswith('.png)'):
            filename=Path(line.rsplit('(',1)[1][:-1]).with_suffix('.drawio')
            story.append(drawio_sketch(filename)); continue
        if line.startswith('```'):
            language=line[3:]; block=[]
            while i<len(lines) and not lines[i].startswith('```'):
                block.append(lines[i]); i+=1
            i+=1
            render=design_erd if name=='design-document' else erd
            story.append(render('\n'.join(block)) if language=='mermaid' else Preformatted('\n'.join(block),styles['code']))
            continue
        if line.startswith('|'):
            rows=[line]
            while i<len(lines) and lines[i].strip().startswith('|'):
                rows.append(lines[i].strip()); i+=1
            rows=[r for r in rows if not re.match(r'^\|[\s:|\-]+\|$',r)]
            cells=[[c.strip() for c in r.strip('|').split('|')] for r in rows]
            if cells[0]==['Field','Details']:
                table=Table([[p('**'+k+'**' if k else '', 'cell'),p(v,'cell')] for k,v in cells[1:]],colWidths=[152,316],hAlign='LEFT')
                table.setStyle(TableStyle([('GRID',(0,0),(-1,-1),0.6,colors.black),('BACKGROUND',(0,0),(0,-1),colors.HexColor('#EFEADE')),('VALIGN',(0,0),(-1,-1),'MIDDLE'),('TOPPADDING',(0,0),(-1,-1),3),('BOTTOMPADDING',(0,0),(-1,-1),3)]))
                story.extend([table,Spacer(1,12)]); continue
            count=len(cells[0]); widths={2:[160,308],3:[97,140,231],4:[50,72,55,291]}.get(count,[468/count]*count)
            table=Table([[p(c,'headcell' if ri==0 else 'cell') for c in row] for ri,row in enumerate(cells)],colWidths=widths,repeatRows=1,hAlign='LEFT')
            table.setStyle(TableStyle([('BACKGROUND',(0,0),(-1,0),NAVY),('GRID',(0,0),(-1,-1),0.4,colors.HexColor('#a7afbc')),
                ('VALIGN',(0,0),(-1,-1),'TOP'),('LEFTPADDING',(0,0),(-1,-1),6),('RIGHTPADDING',(0,0),(-1,-1),6),
                ('TOPPADDING',(0,0),(-1,-1),6),('BOTTOMPADDING',(0,0),(-1,-1),6),
                ('ROWBACKGROUNDS',(0,1),(-1,-1),[colors.white,colors.HexColor('#f4f3ef')])]))
            story.extend([table,Spacer(1,8)]); continue
        if line.startswith('# '): story.append(p(line[2:],'title')); continue
        if line.startswith('## '):
            item=p(line[3:],'h1'); item.toc=bool(re.match(r'\d|Appendix|Revision History',line[3:])); rule=HRFlowable(width='100%',thickness=0.8,color=GOLD,spaceAfter=9); rule.keepWithNext=True; story.extend([item,rule]); continue
        if line.startswith('### '):
            item=p(line[4:],'h2'); story.append(item); continue
        if line.startswith('Table '):
            caption=p(line,'caption'); caption.keepWithNext=True; story.append(caption); continue
        if line.startswith('Figure '): story.append(p(line,'caption')); continue
        if line.startswith('- '): story.append(p('• '+line[2:])); continue
        while i<len(lines) and lines[i].strip() and not re.match(r'^(#|\||```|<!--|- |Table |Figure )',lines[i]):
            line+=' '+lines[i].strip(); i+=1
        story.append(p(line))
    Document(ROOT/f'{name}.pdf', lines[0].lstrip('# ')).multiBuild(story)
    print(ROOT/f'{name}.pdf')

if __name__=='__main__':
    export('design-document')
    export('proposal')
