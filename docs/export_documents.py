"""Export the two Markdown masters to PDF. Requires reportlab and Calibri fonts.

Run: python docs/export_documents.py
Set CALIBRI_DIR if Calibri is not in C:/Windows/Fonts.
The renderer reads document text, tables, SQL and ERD fields from Markdown only.
"""
from pathlib import Path
import os
import re
from html import escape
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.lib import colors
from reportlab.lib.styles import ParagraphStyle
from reportlab.platypus import (BaseDocTemplate, PageTemplate, Frame, Paragraph,
                               Spacer, Table, TableStyle, PageBreak, Preformatted, KeepTogether)
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
    'h1': ParagraphStyle('h1', fontName='Calibri-Bold', fontSize=16, leading=19, textColor=NAVY, spaceAfter=10, keepWithNext=True),
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

class Document(BaseDocTemplate):
    def __init__(self, target, title):
        super().__init__(str(target), pagesize=(612,792), leftMargin=72, rightMargin=72,
                         topMargin=72, bottomMargin=72, title=title, author='Group 1')
        self.addPageTemplates(PageTemplate(id='main', frames=[Frame(72,72,468,648,
            leftPadding=0,rightPadding=0,topPadding=0,bottomPadding=0)], onPage=self.footer))
    def footer(self, canvas, doc):
        canvas.setFont('Calibri',8)
        canvas.setFillColor(NAVY)
        canvas.drawCentredString(306,42,f'CPE 314 | Software Design  -  University of Cebu  -  Richyll Son  -  Page {doc.page}')
    def afterFlowable(self, item):
        if isinstance(item,Paragraph) and getattr(item,'toc',False):
            self.notify('TOCEntry',(0 if item.style.name=='h1' else 1,item.getPlainText(),self.page))

def export(name):
    lines=(ROOT/f'{name}.md').read_text(encoding='utf-8').splitlines()
    story=[]; i=0
    while i<len(lines):
        line=lines[i].strip(); i+=1
        if not line: continue
        if line=='<!-- pagebreak -->': story.append(PageBreak()); continue
        if line=='<!-- toc -->':
            toc=TableOfContents(); toc.levelStyles=[ParagraphStyle('toc0',parent=styles['body'],spaceBefore=3),
                ParagraphStyle('toc1',parent=styles['body'],leftIndent=14)]
            story.append(toc); continue
        if line.startswith('<!--'): continue
        if line.startswith('```'):
            language=line[3:]; block=[]
            while i<len(lines) and not lines[i].startswith('```'):
                block.append(lines[i]); i+=1
            i+=1
            story.append(erd('\n'.join(block)) if language=='mermaid' else Preformatted('\n'.join(block),styles['code']))
            continue
        if line.startswith('|'):
            rows=[line]
            while i<len(lines) and lines[i].strip().startswith('|'):
                rows.append(lines[i].strip()); i+=1
            rows=[r for r in rows if not re.match(r'^\|[\s:|\-]+\|$',r)]
            cells=[[c.strip() for c in r.strip('|').split('|')] for r in rows]
            count=len(cells[0]); widths={2:[160,308],3:[97,140,231],4:[50,72,55,291]}.get(count,[468/count]*count)
            table=Table([[p(c,'headcell' if ri==0 else 'cell') for c in row] for ri,row in enumerate(cells)],colWidths=widths,repeatRows=1,hAlign='LEFT')
            table.setStyle(TableStyle([('BACKGROUND',(0,0),(-1,0),NAVY),('GRID',(0,0),(-1,-1),0.4,colors.HexColor('#a7afbc')),
                ('VALIGN',(0,0),(-1,-1),'TOP'),('LEFTPADDING',(0,0),(-1,-1),6),('RIGHTPADDING',(0,0),(-1,-1),6),
                ('TOPPADDING',(0,0),(-1,-1),6),('BOTTOMPADDING',(0,0),(-1,-1),6),
                ('ROWBACKGROUNDS',(0,1),(-1,-1),[colors.white,colors.HexColor('#f4f3ef')])]))
            story.extend([table,Spacer(1,8)]); continue
        if line.startswith('# '): story.append(p(line[2:],'title')); continue
        if line.startswith('## '):
            item=p(line[3:],'h1'); item.toc=bool(re.match(r'\d|Appendix|Revision History',line[3:])); story.append(item); continue
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
