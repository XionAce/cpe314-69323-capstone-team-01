"""Export the editable design master to PDF. Requires reportlab and Calibri fonts.

Run: python tools/export_design.py
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
from reportlab.graphics.shapes import Drawing, Rect, String, Line, Polygon

ROOT = Path(__file__).resolve().parent.parent / "docs"
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
    'reference': ParagraphStyle('reference',fontName='Calibri',fontSize=10,leading=12,spaceAfter=3),
    'code': ParagraphStyle('code', fontName='Courier', fontSize=9, leading=10, spaceAfter=8),
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

def class_hierarchy(source):
    """Draw the Java inheritance model with separate attribute and method compartments."""
    classes = {}
    for name, body in re.findall(r'class (\w+)\s*\{([^}]+)\}', source):
        attributes, methods = [], []
        for raw in body.splitlines():
            value = raw.strip()
            if not value.startswith(('-', '+')):
                continue
            if '(' in value:
                visibility, signature, result = re.fullmatch(r'([+])([^ ]+) (\w+\*?)', value).groups()
                methods.append(visibility + ' ' + signature + ': ' + result)
            else:
                visibility, kind, field = re.fullmatch(r'([-])(\w+) (\w+)', value).groups()
                attributes.append(visibility + ' ' + field + ': ' + kind)
        classes[name] = (attributes, methods)
    def box_height(name, abstract=False):
        attributes, methods = classes[name]
        return (37 if abstract else 25) + 10 + len(attributes)*12 + 9 + len(methods)*12 + 7
    width = 468
    height = 5 + box_height('Listing', True) + 47 + max(
        box_height(name) for name in ('Textbook', 'Electronics', 'Furniture')) + 5
    drawing = Drawing(width, height)
    ink = colors.HexColor('#456281')
    def line(x1,y1,x2,y2):
        drawing.add(Line(x1,height-y1,x2,height-y2,strokeColor=ink,strokeWidth=1))
    def box(name,x,top,w,abstract=False):
        attributes, methods = classes[name]
        header = 37 if abstract else 25
        row = 12
        h = box_height(name, abstract)
        drawing.add(Rect(x,height-top-h,w,h,fillColor=colors.white,strokeColor=NAVY,strokeWidth=.8))
        drawing.add(Rect(x,height-top-header,w,header,fillColor=NAVY,strokeColor=NAVY))
        if abstract:
            drawing.add(String(x+w/2,height-top-12,'<<abstract>>',fontName='Calibri',fontSize=9,
                               textAnchor='middle',fillColor=colors.white))
        drawing.add(String(x+w/2,height-top-header+8,name,fontName='Calibri-Bold',fontSize=12,
                           textAnchor='middle',fillColor=colors.white))
        size = 10 if abstract else 8.4
        for index, value in enumerate(attributes):
            assert pdfmetrics.stringWidth(value,'Calibri',size) < w-12, value
            drawing.add(String(x+6,height-top-header-15-index*row,value,fontName='Calibri',fontSize=size))
        divider = top+header+len(attributes)*row+9
        line(x,divider,x+w,divider)
        for index,value in enumerate(methods):
            assert pdfmetrics.stringWidth(value,'Calibri',size) < w-12, value
            drawing.add(String(x+6,height-divider-15-index*row,value,fontName='Calibri',fontSize=size))
        return top+h
    bottom=box('Listing',74,5,320,True)
    bus=bottom+27
    child_top=bus+20
    for name,x in [('Textbook',0),('Electronics',160),('Furniture',320)]:
        box(name,x,child_top,148)
        line(x+74,child_top,x+74,bus)
    line(74,bus,394,bus)
    line(234,bus,234,bottom+11)
    drawing.add(Polygon([234,height-bottom,229,height-bottom-11,239,height-bottom-11],
                        fillColor=colors.white,strokeColor=ink,strokeWidth=1))
    return drawing

def design_erd(source):
    if source.strip().startswith('classDiagram'):
        return class_hierarchy(source)
    """Connect schematic keys to the exact row centres in each database view."""
    fields = {}
    for table, body in re.findall(r'(\w+)\s*\{([^}]+)\}',source):
        fields[table] = [re.fullmatch(r'(\w+) (\w+)(?: ([A-Z,]+))?(?: "([^"]+)")?',s.strip()).groups()
                         for s in body.splitlines() if s.strip()]
    if len(fields) >= 10:
        return overview_erd(fields)
    if 'Messages' in fields:
        xy = {'Members':(30,6),'Sellers':(260,6),'Listings':(260,195),'Messages':(30,220)}
        links = [('Members','Sellers','memberId',1,0,235),
                 ('Sellers','Listings','sellerId',1,1,457),
                 ('Listings','Messages','listingId',0,1,245),
                 ('Members','Messages','buyerId',0,0,16),
                 ('Members','Messages','senderId',0,0,5)]
    elif 'Members' in fields:
        xy = {'Members':(31,6),'Sellers':(261,6),'Reviews':(31,200),'Listings':(261,200),'BuyerInterests':(31,340)}
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
        if 'Messages' in fields and child == 'Messages' and parent == 'Members':
            txt(lane+3,190 if fk=='buyerId' else 210,cardinality,9,True)
        elif 'Members' in fields and fk in ('reviewerId','buyerId','sellerId'):
            txt(lane+4 if not side else lane-4,328 if fk=="buyerId" else 181,cardinality,10,True,'start' if not side else 'end')
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
            txt(x+35,top+12,key,9.5)
            txt(x+w-3,top+12,typ,8,False,'end')
    return d

def overview_erd(fields):
    """Overview FK links use exact PK/FK row ports; bridges denote crossings."""
    from reportlab.graphics.shapes import Path as VectorPath, Circle
    xy = {
        'Members': (12, 6), 'Sellers': (174, 6), 'Listings': (336, 6),
        'Reviews': (12, 146), 'BuyerInterests': (174, 146), 'Categories': (336, 146),
        'TextbookDetails': (12, 290), 'ElectronicsDetails': (174, 290),
        'HistoricalSales': (336, 290), 'FurnitureDetails': (174, 405),
        'Messages': (12, 405),
    }
    width, height, head, row = 468, 488, 20, 14
    boxw = 120
    drawing = Drawing(width, height)
    ink = colors.HexColor('#456281')
    def port(table, field, side):
        i = next(i for i, f in enumerate(fields[table]) if f[1] == field)
        x, y = xy[table]
        return (x + side * boxw, y + head + (i + .5) * row)
    def line(a, b, color=ink, weight=.9):
        drawing.add(Line(a[0], height-a[1], b[0], height-b[1],
                         strokeColor=color, strokeWidth=weight))
    def text(x, y, value, size=8, bold=False, color=NAVY, anchor='start'):
        drawing.add(String(x, height-y, value, fontName='Calibri-Bold' if bold else 'Calibri',
                           fontSize=size, fillColor=color, textAnchor=anchor))
    edges = [
        ('Members', 'Sellers', 'memberId', 1, 0, 153, (139, 24)),
        ('Members', 'Reviews', 'reviewerId', 0, 0, 4, (2, 125)),
        ('Members', 'BuyerInterests', 'buyerId', 1, 0, 145, (148, 125)),
        ('Sellers', 'Listings', 'sellerId', 1, 0, 315, (298, 24)),
        ('Listings', 'Reviews', 'listingId', 0, 1, None, (200, 106)),
        ('Listings', 'BuyerInterests', 'listingId', 0, 1, 321, (298, 128)),
        ('Categories', 'Listings', 'categoryId', 0, 0, 307, (285, 95)),
        ('Categories', 'Categories', 'parentId', 0, 0, 327, (313, 211)),
        ('Categories', 'HistoricalSales', 'categoryId', 0, 0, 310, (291, 247)),
        ('Listings', 'TextbookDetails', 'listingId', 0, 1, None, (48, 251)),
        ('Listings', 'ElectronicsDetails', 'listingId', 0, 1, 321, (299, 278)),
        ('Listings', 'FurnitureDetails', 'listingId', 1, 1, 464, (441, 389)),
        ('Listings', 'Messages', 'listingId', 1, 1, 464, (334, 460)),
        ('Members', 'Messages', 'buyerId', 0, 0, 4, (2, 370)),
        ('Members', 'Messages', 'senderId', 0, 0, 4, (2, 392)),
    ]
    segments = []
    for index, (parent, child, key, side, entry, lane, label) in enumerate(edges):
        a = port(parent, 'id', side)
        b = port(child, key, entry)
        if lane is None and child == 'Reviews':
            route = [a, (321, a[1]), (321, 110), (153, 110), (153, b[1]), b]
        elif lane is None:
            route = [a, (321, a[1]), (321, 255), (144, 255), (144, b[1]), b]
        else:
            route = [a, (lane, a[1]), (lane, b[1]), b]
        for start, end in zip(route, route[1:]):
            line(start, end)
            segments.append((index, parent, start, end))
        # A bar at the parent; crow's foot or a second bar at the child.
        px = a[0] + (5 if side else -5)
        line((px, a[1]-3), (px, a[1]+3))
        fork = b[0] + (7 if entry else -7)
        if child.endswith('Details'):
            line((fork, b[1]-3), (fork, b[1]+3))
        else:
            line((fork, b[1]), (b[0], b[1]-3))
            line((fork, b[1]), (b[0], b[1]+3))
        text(*label, '1:1' if child.endswith('Details') else '1:M', 8, True)
    # Mark unrelated line crossings explicitly instead of suggesting a junction.
    jumps = set()
    for i, parent, a, b in segments:
        if a[1] != b[1]:
            continue
        for j, other, c, e in segments:
            if i == j or parent == other or c[0] != e[0]:
                continue
            x, y = c[0], a[1]
            if min(a[0], b[0])+4 < x < max(a[0], b[0])-4 and min(c[1], e[1])+4 < y < max(c[1], e[1])-4:
                jumps.add((x, y))
    for x, y in sorted(jumps):
        drawing.add(Circle(x, height-y, 3.2, fillColor=colors.white, strokeColor=None))
        line((x, y-4), (x, y+4))
        path = VectorPath(strokeColor=ink, strokeWidth=.9, fillColor=None)
        path.moveTo(x-4, height-y)
        path.curveTo(x-3, height-y+4, x+3, height-y+4, x+4, height-y)
        drawing.add(path)
    for name, (x, y) in xy.items():
        drawing.add(Rect(x, height-y-head, boxw, head, fillColor=NAVY, strokeColor=NAVY))
        text(x+boxw/2, y+13, name, 9.5, True, colors.white, 'middle')
        for i, (typ, key, flags, alias) in enumerate(fields[name]):
            top = y+head+i*row
            drawing.add(Rect(x, height-top-row, boxw, row, fillColor=colors.white,
                             strokeColor=NAVY, strokeWidth=.5))
            line((x+24, top), (x+24, top+row), NAVY, .5)
            text(x+2, top+10, flags or '', 7)
            text(x+28, top+10, key, 9)
    return drawing


class Document(BaseDocTemplate):
    def __init__(self, target, title):
        super().__init__(str(target), pagesize=(612,792), leftMargin=72, rightMargin=72,
                         topMargin=72, bottomMargin=72, title=title, author='Group 1')
        self.addPageTemplates(PageTemplate(id='main', frames=[Frame(72,72,468,648,
            leftPadding=0,rightPadding=0,topPadding=0,bottomPadding=0)], onPageEnd=self.footer))
    def beforeDocument(self):
        self.intro_page = None
    def footer(self, canvas, doc):
        design = Path(self.filename).stem=='design-document'
        if design and self.intro_page is None:
            return
        number=doc.page-self.intro_page+1 if design else doc.page
        canvas.setFont('Calibri',8)
        canvas.setFillColor(NAVY)
        canvas.drawCentredString(306,42,f'CPE 314 | Software Design   ·   University of Cebu   ·   Richyll Son   ·   Page {number}')
    def afterFlowable(self, item):
        if isinstance(item, Paragraph) and item.getPlainText().startswith('1. Introduction'):
            self.intro_page = self.page
        if isinstance(item,Paragraph) and getattr(item,'toc',False):
            page=self.page
            if Path(self.filename).stem=='design-document':
                if self.intro_page is None: return
                page=page-self.intro_page+1
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
            toc=TableOfContents(); toc.levelStyles=[ParagraphStyle('toc0',fontName='Calibri-Bold',fontSize=10.5,leading=12,spaceBefore=3,textColor=NAVY),
                ParagraphStyle('toc1',fontName='Calibri',fontSize=9.5,leading=11,leftIndent=16,spaceBefore=0,spaceAfter=0)]
            toc.tableStyle=TableStyle([('TOPPADDING',(0,0),(-1,-1),0),('BOTTOMPADDING',(0,0),(-1,-1),0)])
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
            render=design_erd
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
            compact=cells[0][0]=='Process'
            if compact: widths=[96,156,216]
            def content(c, ri):
                style=styles['headcell' if ri==0 else 'cell']
                if compact: style=ParagraphStyle('mapcell',parent=style,fontSize=10,leading=11)
                return Paragraph(inline(c),style)
            table=Table([[content(c,ri) for c in row] for ri,row in enumerate(cells)],colWidths=widths,repeatRows=1,hAlign='LEFT')
            table.setStyle(TableStyle([('BACKGROUND',(0,0),(-1,0),NAVY),('GRID',(0,0),(-1,-1),0.4,colors.HexColor('#a7afbc')),
                ('VALIGN',(0,0),(-1,-1),'TOP'),('LEFTPADDING',(0,0),(-1,-1),6),('RIGHTPADDING',(0,0),(-1,-1),6),
                ('TOPPADDING',(0,0),(-1,-1),6),('BOTTOMPADDING',(0,0),(-1,-1),6),
                ('ROWBACKGROUNDS',(0,1),(-1,-1),[colors.white,colors.HexColor('#f4f3ef')])]))
            if compact or cells[0][0]=='Member and role': table.setStyle(TableStyle([('TOPPADDING',(0,0),(-1,-1),4),('BOTTOMPADDING',(0,0),(-1,-1),4)]))
            story.extend([table,Spacer(1,8)]); continue
        if line.startswith('# '): story.append(p(line[2:],'title')); continue
        if line.startswith('## '):
            item=p(line[3:],'h1'); item.toc=bool(re.match(r'\d|Appendix|Revision History',line[3:])); rule=HRFlowable(width='100%',thickness=0.8,color=GOLD,spaceAfter=9); rule.keepWithNext=True; story.extend([item,rule]); continue
        if line.startswith('### '):
            item=p(line[4:],'h2'); item.toc=(' ' in line[4:] or line[4:] == 'Verification'); story.append(item); continue
        if line.startswith('Table '):
            caption=p(line,'caption'); caption.keepWithNext=True; story.append(caption); continue
        if line.startswith('Figure '): story.append(p(line,'caption')); continue
        if line.startswith('- '): story.append(p('• '+line[2:],'reference' if line.startswith('- [') else 'body')); continue
        while i<len(lines) and lines[i].strip() and not re.match(r'^(#|\||```|<!--|- |Table |Figure )',lines[i]):
            line+=' '+lines[i].strip(); i+=1
        story.append(p(line))
    Document(ROOT/f'{name}.pdf', next(l[2:] for l in lines if l.startswith('# '))).multiBuild(story)
    print(ROOT/f'{name}.pdf')

if __name__=='__main__':
    export('design-document')
