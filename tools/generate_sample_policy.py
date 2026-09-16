"""Generate the small sample PDF used by the local RAG demo."""

from pathlib import Path

from reportlab.lib.colors import HexColor
from reportlab.lib.enums import TA_CENTER
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.platypus import PageBreak, Paragraph, SimpleDocTemplate, Spacer


OUTPUT = Path(__file__).parents[1] / "src" / "main" / "resources" / "company_policy.pdf"


def footer(canvas, document):
    canvas.saveState()
    canvas.setStrokeColor(HexColor("#D7D1C4"))
    canvas.line(22 * mm, 16 * mm, 188 * mm, 16 * mm)
    canvas.setFont("Helvetica", 8)
    canvas.setFillColor(HexColor("#687A73"))
    canvas.drawString(22 * mm, 10 * mm, "Northstar Labs - Employee Policy")
    canvas.drawRightString(188 * mm, 10 * mm, f"Page {document.page}")
    canvas.restoreState()


def build_pdf():
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    document = SimpleDocTemplate(
        str(OUTPUT),
        pagesize=A4,
        leftMargin=25 * mm,
        rightMargin=25 * mm,
        topMargin=24 * mm,
        bottomMargin=24 * mm,
        title="Northstar Labs Employee Policy",
        author="Northstar Labs",
    )

    styles = getSampleStyleSheet()
    title = ParagraphStyle(
        "TitleCustom",
        parent=styles["Title"],
        fontName="Helvetica-Bold",
        fontSize=28,
        leading=32,
        textColor=HexColor("#174E3F"),
        alignment=TA_CENTER,
        spaceAfter=8 * mm,
    )
    subtitle = ParagraphStyle(
        "SubtitleCustom",
        parent=styles["Normal"],
        fontSize=11,
        leading=16,
        textColor=HexColor("#687A73"),
        alignment=TA_CENTER,
        spaceAfter=18 * mm,
    )
    heading = ParagraphStyle(
        "HeadingCustom",
        parent=styles["Heading2"],
        fontName="Helvetica-Bold",
        fontSize=17,
        leading=24,
        textColor=HexColor("#174E3F"),
        spaceBefore=6 * mm,
        spaceAfter=5 * mm,
    )
    body = ParagraphStyle(
        "BodyCustom",
        parent=styles["BodyText"],
        fontName="Helvetica",
        fontSize=10.5,
        leading=16,
        textColor=HexColor("#243B34"),
        spaceAfter=4 * mm,
    )

    story = [
        Spacer(1, 28 * mm),
        Paragraph("Northstar Labs", title),
        Paragraph("EMPLOYEE POLICY HANDBOOK<br/>Sample document for the Java RAG demonstration", subtitle),
        Paragraph("Effective date: 1 January 2026", body),
        Paragraph(
            "This sample handbook exists only to demonstrate document ingestion and grounded question answering. "
            "Replace it with your organization's approved policy before using the application for real decisions.",
            body,
        ),
        PageBreak(),
        Spacer(1, 8 * mm),
        Paragraph("1. Annual leave", heading),
        Paragraph(
            "Full-time employees receive 20 paid annual-leave days per calendar year. Leave accrues monthly "
            "from the employee's start date. Up to 5 unused days may be carried into the next calendar year; "
            "carried leave must be used by 31 March or it expires.",
            body,
        ),
        Paragraph(
            "Requests for 1 or 2 consecutive days should be submitted at least 3 working days in advance. "
            "Requests for 3 or more consecutive days should be submitted at least 10 working days in advance. "
            "A manager may approve exceptions for emergencies.",
            body,
        ),
        Paragraph("2. Remote work", heading),
        Paragraph(
            "Employees may work remotely for up to 3 days each week when their role and team commitments allow it. "
            "The employee and manager must agree on a regular remote-work schedule in writing. Core collaboration "
            "hours are 10:00 to 15:00 in the employee's local time zone.",
            body,
        ),
        Paragraph(
            "Working outside the employee's country of employment requires written approval from Human Resources "
            "and the Legal team before travel is booked. Approval is not automatic because tax, security, and "
            "immigration rules may apply.",
            body,
        ),
        Paragraph("3. Code of conduct", heading),
        Paragraph(
            "Everyone must treat colleagues, customers, and partners with respect. Harassment, discrimination, "
            "retaliation, threats, and deliberate misuse of company systems are prohibited. Suspected misconduct "
            "may be reported to a manager, Human Resources, or the confidential ethics channel.",
            body,
        ),
        Paragraph(
            "Employees must protect confidential information and use the minimum access necessary for their work. "
            "Company or customer data may not be copied to personal accounts, unapproved storage, or public AI tools.",
            body,
        ),
        Paragraph("4. Sick leave", heading),
        Paragraph(
            "Employees receive 10 paid sick-leave days each calendar year. Notify the manager as soon as reasonably "
            "possible. A medical certificate may be requested after 3 consecutive working days of absence, subject "
            "to local law.",
            body,
        ),
    ]

    document.build(story, onFirstPage=footer, onLaterPages=footer)


if __name__ == "__main__":
    build_pdf()
    print(OUTPUT)
