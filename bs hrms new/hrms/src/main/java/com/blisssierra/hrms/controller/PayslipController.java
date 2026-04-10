package com.blisssierra.hrms.controller;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.blisssierra.hrms.entity.Employee;
import com.blisssierra.hrms.entity.Salary;
import com.blisssierra.hrms.repository.EmployeeRepository;
import com.blisssierra.hrms.repository.SalaryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.Month;

@RestController
@RequestMapping("/api/payslip")
@CrossOrigin("*")
public class PayslipController {
        @Autowired
        private SalaryRepository salaryRepo;
        @Autowired
        private EmployeeRepository employeeRepo;

        // ✅ GET LAST MONTH PAYSLIP DATA
        @GetMapping("/{empId}")
        public ResponseEntity<?> getLastMonthPayslip(@PathVariable Long empId) {
                // ── Calculate last month ──────────────────────────────
                LocalDate today = LocalDate.now();
                LocalDate lastMonth = today.minusMonths(1);
                int month = lastMonth.getMonthValue();
                int year = lastMonth.getYear();
                return buildPayslipResponse(empId, month, year);
        }

        // ✅ GET SPECIFIC MONTH PAYSLIP (for history screen)
        @GetMapping("/{empId}/{month}/{year}")
        public ResponseEntity<?> getPayslip(
                        @PathVariable Long empId,
                        @PathVariable int month,
                        @PathVariable int year) {
                return buildPayslipResponse(empId, month, year);
        }

        // ✅ DOWNLOAD LAST MONTH PDF
        @GetMapping("/download/{empId}")
        public ResponseEntity<byte[]> downloadLastMonthPdf(@PathVariable Long empId) {
                LocalDate today = LocalDate.now();
                LocalDate lastMonth = today.minusMonths(1);
                return generatePdf(empId, lastMonth.getMonthValue(), lastMonth.getYear());
        }

        // ✅ DOWNLOAD SPECIFIC MONTH PDF
        @GetMapping("/download/{empId}/{month}/{year}")
        public ResponseEntity<byte[]> downloadPdf(
                        @PathVariable Long empId,
                        @PathVariable int month,
                        @PathVariable int year) {
                return generatePdf(empId, month, year);
        }

        @GetMapping("/all-employees")
        public ResponseEntity<?> getAllEmployees() {
                return ResponseEntity.ok(
                                employeeRepo.findAll()
                                                .stream()
                                                .map(e -> java.util.Map.of(
                                                                "id", e.getId(),
                                                                "name", e.getName(),
                                                                "empCode", e.getEmpCode()))
                                                .collect(java.util.stream.Collectors.toList()));
        }

        // ── Build JSON response ───────────────────────────────────
        private ResponseEntity<?> buildPayslipResponse(Long empId, int month, int year) {
                Employee emp = employeeRepo.findById(empId)
                                .orElseThrow(() -> new RuntimeException("Employee not found"));
                // 🔍 Fetch last month salary record
                Salary salary = salaryRepo
                                .findByEmployeeIdAndMonthAndYear(empId, month, year)
                                .orElse(null);
                if (salary == null || salary.getPresentDays() == 0) {
                        return ResponseEntity.ok(java.util.Map.of(
                                        "message", "No attendance found for " + Month.of(month).name() + " " + year,
                                        "earnedSalary", 0,
                                        "presentDays", 0));
                }
                // ── Calculations based on ACTUAL earned salary ────────
                double earned = salary.getEarnedSalary(); // e.g. 26 days × ₹612 = ₹15,912
                double perDayRate = 612.0;
                double EPF = 900; // 5% tax
                double insurance = 200.0;
                double totalDeductions = EPF + insurance;
                double netSalary = earned - totalDeductions;
                // Earnings breakdown (proportional to earned)F
                double basic = earned * 0.50;
                double hra = earned * 0.20;
                double transport = earned * 0.10;
                double special = earned * 0.20;
                PayslipResponse res = new PayslipResponse();
                res.employeeName = emp.getName();
                res.empCode = emp.getEmpCode();
                res.month = Month.of(month).name();
                res.year = year;
                res.presentDays = salary.getPresentDays();
                res.perDayRate = perDayRate;
                res.earnedSalary = earned;
                // In buildPayslipResponse(), after setting earnedSalary:
                res.grossSalary = salary.getGrossSalary(); // ADD THIS LINE
                res.basicSalary = basic;
                res.hra = hra;
                res.transport = transport;
                res.specialAllowance = special;
                res.tax = EPF;
                res.insurance = insurance;
                res.totalDeductions = totalDeductions;
                res.netSalary = netSalary;
                return ResponseEntity.ok(res);
        }

        // ── Generate PDF ──────────────────────────────────────────
        // private ResponseEntity<byte[]> generatePdf(Long empId, int month, int year) {
        // Employee emp = employeeRepo.findById(empId)
        // .orElseThrow(() -> new RuntimeException("Employee not found"));
        // // ✅ Return a clean 404 instead of 500
        // Salary salary = salaryRepo
        // .findByEmployeeIdAndMonthAndYear(empId, month, year)
        // .orElse(null);
        // if (salary == null) {
        // return ResponseEntity.status(404)
        // .body(("No salary record found for " + Month.of(month).name() + " " +
        // year).getBytes());
        // }
        // double earned = salary.getEarnedSalary();
        // double tax = earned * 0.05;
        // double insurance = 200.0;
        // double totalDeductions = tax + insurance;
        // double netSalary = earned - totalDeductions;
        // double basic = earned * 0.50;
        // double hra = earned * 0.20;
        // double transport = earned * 0.10;
        // double special = earned * 0.20;
        // String monthName = Month.of(month).name();
        // try {
        // ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // Document document = new Document(PageSize.A4);
        // PdfWriter.getInstance(document, baos);
        // document.open();
        // // ── Colors ────────────────────────────────────────
        // BaseColor darkBlue = new BaseColor(17, 34, 53);
        // BaseColor medBlue = new BaseColor(47, 110, 142);
        // BaseColor lightBlue = new BaseColor(219, 234, 254);
        // BaseColor green = new BaseColor(22, 163, 74);
        // BaseColor red = new BaseColor(220, 38, 38);
        // BaseColor lightGreen = new BaseColor(220, 252, 231);
        // // ── Fonts ─────────────────────────────────────────
        // Font titleFont = new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD,
        // BaseColor.WHITE);
        // Font subFont = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, new
        // BaseColor(180, 200, 220));
        // Font whiteFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD,
        // BaseColor.WHITE);
        // Font labelFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, new
        // BaseColor(80, 80, 80));
        // Font valueFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD,
        // darkBlue);
        // Font greenFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, green);
        // Font redFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, red);
        // Font netFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, green);
        // Font boldRed = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, red);
        // Font boldBlue = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, medBlue);
        // // ── Header ────────────────────────────────────────
        // PdfPTable hdrTable = new PdfPTable(1);
        // hdrTable.setWidthPercentage(100);
        // PdfPCell titleCell = new PdfPCell(new Phrase("SALARY SLIP", titleFont));
        // titleCell.setBackgroundColor(darkBlue);
        // titleCell.setPadding(16);
        // titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        // titleCell.setBorder(Rectangle.NO_BORDER);
        // hdrTable.addCell(titleCell);
        // PdfPCell monthCell = new PdfPCell(new Phrase(monthName + " " + year,
        // subFont));
        // monthCell.setBackgroundColor(darkBlue);
        // monthCell.setPadding(4);
        // monthCell.setPaddingBottom(14);
        // monthCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        // monthCell.setBorder(Rectangle.NO_BORDER);
        // hdrTable.addCell(monthCell);
        // document.add(hdrTable);
        // document.add(Chunk.NEWLINE);
        // // ── Employee Details ──────────────────────────────
        // addSectionHeader(document, "EMPLOYEE DETAILS", medBlue, whiteFont);
        // PdfPTable empTable = new PdfPTable(4);
        // empTable.setWidthPercentage(100);
        // empTable.setWidths(new float[] { 1.2f, 1.8f, 1.2f, 1.8f });
        // addInfoRow(empTable, "Name", emp.getName(), labelFont, valueFont, lightBlue);
        // addInfoRow(empTable, "Employee ID", emp.getEmpCode(), labelFont, valueFont,
        // BaseColor.WHITE);
        // addInfoRow(empTable, "Month", monthName + " " + year, labelFont, valueFont,
        // lightBlue);
        // addInfoRow(empTable, "Days Present", salary.getPresentDays() + " days",
        // labelFont, valueFont,
        // BaseColor.WHITE);
        // addInfoRow(empTable, "Per Day Rate", "₹612", labelFont, valueFont,
        // lightBlue);
        // addInfoRow(empTable, "Department", "Engineering", labelFont, valueFont,
        // BaseColor.WHITE);
        // document.add(empTable);
        // document.add(Chunk.NEWLINE);
        // // ── Attendance Summary Box ────────────────────────
        // PdfPTable attBox = new PdfPTable(3);
        // attBox.setWidthPercentage(100);
        // addSummaryCell(attBox, "Days Present", salary.getPresentDays() + " days",
        // medBlue, lightBlue);
        // addSummaryCell(attBox, "Per Day Rate", "₹612", medBlue, lightBlue);
        // addSummaryCell(attBox, "Total Earned", "₹" + String.format("%.2f", earned),
        // new BaseColor(21, 128, 61),
        // lightGreen);
        // document.add(attBox);
        // document.add(Chunk.NEWLINE);
        // // ── Earnings ──────────────────────────────────────
        // addSectionHeader(document, "EARNINGS BREAKDOWN", medBlue, whiteFont);
        // PdfPTable earnTable = new PdfPTable(2);
        // earnTable.setWidthPercentage(100);
        // addSalaryRow(earnTable, "Basic Salary (50%)", "₹" + fmt(basic), labelFont,
        // greenFont, lightBlue);
        // addSalaryRow(earnTable, "House Rent Allowance (20%)", "₹" + fmt(hra),
        // labelFont, greenFont,
        // BaseColor.WHITE);
        // addSalaryRow(earnTable, "Transport Allowance (10%)", "₹" + fmt(transport),
        // labelFont, greenFont, lightBlue);
        // addSalaryRow(earnTable, "Special Allowance (20%)", "₹" + fmt(special),
        // labelFont, greenFont,
        // BaseColor.WHITE);
        // addSalaryRow(earnTable, "GROSS EARNED SALARY", "₹" + fmt(earned), whiteFont,
        // boldBlue, lightBlue);
        // document.add(earnTable);
        // document.add(Chunk.NEWLINE);
        // // ── Deductions ────────────────────────────────────
        // addSectionHeader(document, "DEDUCTIONS", new BaseColor(180, 60, 60),
        // whiteFont);
        // PdfPTable dedTable = new PdfPTable(2);
        // dedTable.setWidthPercentage(100);
        // addSalaryRow(dedTable, "Income Tax (5%)", "-₹" + fmt(tax), labelFont,
        // redFont, lightBlue);
        // addSalaryRow(dedTable, "Insurance", "-₹" + fmt(insurance), labelFont,
        // redFont, BaseColor.WHITE);
        // addSalaryRow(dedTable, "TOTAL DEDUCTIONS", "-₹" + fmt(totalDeductions),
        // whiteFont, boldRed, lightBlue);
        // document.add(dedTable);
        // document.add(Chunk.NEWLINE);
        // // ── Net Salary ────────────────────────────────────
        // PdfPTable netTable = new PdfPTable(2);
        // netTable.setWidthPercentage(100);
        // PdfPCell netLabel = new PdfPCell(
        // new Phrase("NET SALARY (Take Home)", new Font(Font.FontFamily.HELVETICA, 13,
        // Font.BOLD, darkBlue)));
        // netLabel.setBackgroundColor(lightGreen);
        // netLabel.setPadding(14);
        // netLabel.setBorder(Rectangle.BOX);
        // netLabel.setBorderColor(new BaseColor(187, 247, 208));
        // PdfPCell netValue = new PdfPCell(new Phrase("₹" + fmt(netSalary), netFont));
        // netValue.setBackgroundColor(lightGreen);
        // netValue.setPadding(14);
        // netValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
        // netValue.setBorder(Rectangle.BOX);
        // netValue.setBorderColor(new BaseColor(187, 247, 208));
        // netTable.addCell(netLabel);
        // netTable.addCell(netValue);
        // document.add(netTable);
        // // ── Footer ────────────────────────────────────────
        // document.add(Chunk.NEWLINE);
        // document.add(Chunk.NEWLINE);
        // Font footerFont = new Font(Font.FontFamily.HELVETICA, 9, Font.ITALIC, new
        // BaseColor(150, 150, 150));
        // Paragraph footer = new Paragraph(
        // "This is a system-generated payslip. For queries contact HR. | Generated on "
        // + LocalDate.now(),
        // footerFont);
        // footer.setAlignment(Element.ALIGN_CENTER);
        // document.add(footer);
        // document.close();
        // return ResponseEntity.ok()
        // .header(HttpHeaders.CONTENT_DISPOSITION,
        // "attachment; filename=payslip_" + emp.getEmpCode()
        // + "_" + monthName + "_" + year + ".pdf")
        // .contentType(MediaType.APPLICATION_PDF)
        // .body(baos.toByteArray());
        // } catch (Exception e) {
        // throw new RuntimeException("PDF generation failed: " + e.getMessage());
        // }
        // }
        private ResponseEntity<byte[]> generatePdf(Long empId, int month, int year) {
                Employee emp = employeeRepo.findById(empId)
                                .orElseThrow(() -> new RuntimeException("Employee not found"));
                Salary salary = salaryRepo
                                .findByEmployeeIdAndMonthAndYear(empId, month, year)
                                .orElse(null);
                if (salary == null) {
                        return ResponseEntity.status(404)
                                        .body(("No salary record found for " + Month.of(month).name() + " " + year)
                                                        .getBytes());
                }
                double earned = salary.getEarnedSalary();
                double basic = earned * 0.50;
                double hra = earned * 0.20;
                double specialAllow = earned * 0.10;
                double companyContrib = earned * 0.20;
                double totalEarnings = earned;
                double professionalTax = 200.0;
                double healthCard = 0.0;
                double epf = 900.0;
                double loan = 0.0;
                double totalDeductions = professionalTax + healthCard + epf + loan;
                double netPay = totalEarnings - totalDeductions;
                int workingDays = salary.getPresentDays();
                double daySalary = workingDays > 0 ? earned / workingDays : 0;
                String monthName = Month.of(month).name().substring(0, 1)
                                + Month.of(month).name().substring(1).toLowerCase();
                String monthYear = monthName.substring(0, 3) + "-" + String.valueOf(year).substring(2);
                try {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
                        PdfWriter.getInstance(document, baos);
                        document.open();
                        // ── Colors ────────────────────────────────────────────
                        BaseColor darkGreen = new BaseColor(0, 100, 0);
                        BaseColor lightGreen = new BaseColor(198, 224, 180);
                        BaseColor lightBlue = new BaseColor(189, 215, 238);
                        BaseColor orange = new BaseColor(255, 102, 0);
                        BaseColor black = BaseColor.BLACK;
                        BaseColor white = BaseColor.WHITE;
                        BaseColor lightGray = new BaseColor(242, 242, 242);
                        // ── Fonts ─────────────────────────────────────────────
                        Font companyFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, orange);
                        Font addressFont = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, black);
                        Font boldSmall = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, black);
                        Font normalSmall = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, black);
                        Font boldGreen = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, darkGreen);
                        Font netPayFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, black);
                        // ══════════════════════════════════════════════════════
                        // HEADER — Logo placeholder + Company Name + Address
                        // ══════════════════════════════════════════════════════
                        PdfPTable headerTable = new PdfPTable(new float[] { 1.2f, 3f });
                        headerTable.setWidthPercentage(100);
                        // Logo cell (placeholder box)
                        PdfPCell logoCell = new PdfPCell();
                        logoCell.setBorder(Rectangle.BOX);
                        logoCell.setPadding(10);
                        logoCell.setFixedHeight(70);
                        try {
                                Image logo = Image.getInstance(getClass().getResource("/static/bs image.jpg"));
                                logo.scaleToFit(50, 50); // adjust size
                                logo.setAlignment(Element.ALIGN_CENTER);
                                logoCell.addElement(logo);
                        } catch (Exception e) {
                                e.printStackTrace();
                        }
                        headerTable.addCell(logoCell);
                        // Company name + address cell
                        PdfPCell companyCell = new PdfPCell();
                        companyCell.setBorder(Rectangle.BOX);
                        companyCell.setPadding(8);
                        companyCell.addElement(new Phrase("BLISS SIERRA SOFTWARE SOLUTIONS", companyFont));
                        companyCell.addElement(new Phrase(
                                        "Flat No 402, Neelakanta Nilayam, Kakatiya Hills, Road No 9, Guttalabegumpet, Madhapur, Hyderabad-500081",
                                        addressFont));
                        headerTable.addCell(companyCell);
                        document.add(headerTable);
                        // ══════════════════════════════════════════════════════
                        // PAY PERIOD + PAY SLIP TITLE ROW
                        // ══════════════════════════════════════════════════════
                        PdfPTable periodTable = new PdfPTable(new float[] { 1.5f, 1.5f, 2f });
                        periodTable.setWidthPercentage(100);
                        PdfPCell periodLabelCell = new PdfPCell(new Phrase("Pay Period (Month)", boldSmall));
                        periodLabelCell.setBorder(Rectangle.BOX);
                        periodLabelCell.setPadding(6);
                        periodLabelCell.setBackgroundColor(white);
                        periodTable.addCell(periodLabelCell);
                        PdfPCell periodValueCell = new PdfPCell(new Phrase(monthYear, boldGreen));
                        periodValueCell.setBorder(Rectangle.BOX);
                        periodValueCell.setPadding(6);
                        periodValueCell.setBackgroundColor(lightBlue);
                        periodValueCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                        periodTable.addCell(periodValueCell);
                        PdfPCell paySlipTitleCell = new PdfPCell(
                                        new Phrase("PAY SLIP",
                                                        new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, black)));
                        paySlipTitleCell.setBorder(Rectangle.BOX);
                        paySlipTitleCell.setPadding(6);
                        paySlipTitleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                        paySlipTitleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                        periodTable.addCell(paySlipTitleCell);
                        document.add(periodTable);
                        // ══════════════════════════════════════════════════════
                        // EMPLOYEE DETAILS SECTION HEADER
                        // ══════════════════════════════════════════════════════
                        PdfPTable empHeaderTable = new PdfPTable(1);
                        empHeaderTable.setWidthPercentage(100);
                        PdfPCell empHeaderCell = new PdfPCell(new Phrase("EMPLOYEE DETAILS", boldSmall));
                        empHeaderCell.setBackgroundColor(lightBlue);
                        empHeaderCell.setBorder(Rectangle.BOX);
                        empHeaderCell.setPadding(5);
                        empHeaderCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                        empHeaderTable.addCell(empHeaderCell);
                        document.add(empHeaderTable);
                        // ══════════════════════════════════════════════════════
                        // EMPLOYEE DETAILS ROWS
                        // ══════════════════════════════════════════════════════
                        PdfPTable empDetailsTable = new PdfPTable(new float[] { 1.5f, 2f, 1.5f, 2f });
                        empDetailsTable.setWidthPercentage(100);
                        addEmpRow(empDetailsTable, "Employee ID", emp.getEmpCode(), "", "", boldSmall,
                                        boldGreen,
                                        normalSmall, lightGray);
                        addEmpRow(empDetailsTable, "Name", emp.getName(), "Designation", "Software Engineer", boldSmall,
                                        normalSmall, normalSmall, white);
                        addEmpRow(empDetailsTable, "Gross Salary", "₹" + fmt(salary.getGrossSalary()),
                                        "Earned Salary", "₹" + fmt(earned), boldSmall, boldGreen, normalSmall, white);
                        document.add(empDetailsTable);
                        // ══════════════════════════════════════════════════════
                        // EARNINGS + DEDUCTIONS TWO-COLUMN TABLE
                        // ══════════════════════════════════════════════════════
                        document.add(Chunk.NEWLINE);
                        PdfPTable mainTable = new PdfPTable(new float[] { 2f, 1f, 2f, 1f });
                        mainTable.setWidthPercentage(100);
                        // Header row
                        PdfPCell earnHeader = new PdfPCell(new Phrase("EARNINGS", boldSmall));
                        earnHeader.setBackgroundColor(lightGreen);
                        earnHeader.setBorder(Rectangle.BOX);
                        earnHeader.setPadding(5);
                        earnHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
                        PdfPCell earnAmtHeader = new PdfPCell(new Phrase("", boldSmall));
                        earnAmtHeader.setBackgroundColor(lightGreen);
                        earnAmtHeader.setBorder(Rectangle.BOX);
                        PdfPCell dedHeader = new PdfPCell(new Phrase("DEDUCTIONS", boldSmall));
                        dedHeader.setBackgroundColor(lightGreen);
                        dedHeader.setBorder(Rectangle.BOX);
                        dedHeader.setPadding(5);
                        dedHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
                        PdfPCell dedAmtHeader = new PdfPCell(new Phrase("", boldSmall));
                        dedAmtHeader.setBackgroundColor(lightGreen);
                        dedAmtHeader.setBorder(Rectangle.BOX);
                        mainTable.addCell(earnHeader);
                        mainTable.addCell(earnAmtHeader);
                        mainTable.addCell(dedHeader);
                        mainTable.addCell(dedAmtHeader);
                        // Data rows
                        addMainRow(mainTable, "Basic Salary", fmt(basic), "Professional Tax", fmt(professionalTax),
                                        normalSmall,
                                        boldSmall, lightGray, white);
                        addMainRow(mainTable, "HRA", fmt(hra), "Health Card", fmt(healthCard), normalSmall, boldSmall,
                                        white,
                                        lightGray);
                        addMainRow(mainTable, "Special Allowance", fmt(specialAllow), "EPF", fmt(epf), normalSmall,
                                        boldSmall,
                                        lightGray, white);
                        addMainRow(mainTable, "Company Contribution", fmt(companyContrib), "Loan", fmt(loan),
                                        normalSmall,
                                        boldSmall, white, lightGray);
                        document.add(mainTable);
                        // ══════════════════════════════════════════════════════
                        // TOTALS TABLE
                        // ══════════════════════════════════════════════════════
                        PdfPTable totalsTable = new PdfPTable(new float[] { 2f, 1f, 2f, 1f });
                        totalsTable.setWidthPercentage(100);
                        addTotalRow(totalsTable, "Total Earnings", fmt(totalEarnings), "Total Deductions",
                                        fmt(totalDeductions),
                                        boldSmall, lightGreen);
                        addTotalRow(totalsTable, "Working Days", String.valueOf(workingDays), "Day Salary",
                                        fmt(daySalary),
                                        boldSmall, white);
                        addTotalRow(totalsTable, "Total Amount", fmt(earned), "", "", boldSmall, lightGray);
                        document.add(totalsTable);
                        // ══════════════════════════════════════════════════════
                        // NET PAY ROW
                        // ══════════════════════════════════════════════════════
                        PdfPTable netTable = new PdfPTable(new float[] { 2f, 1f, 2f, 1f });
                        netTable.setWidthPercentage(100);
                        PdfPCell netLabel = new PdfPCell(new Phrase("NET PAY", netPayFont));
                        netLabel.setBorder(Rectangle.BOX);
                        netLabel.setPadding(8);
                        netLabel.setBackgroundColor(lightGreen);
                        PdfPCell netValue = new PdfPCell(new Phrase(fmt(netPay), netPayFont));
                        netValue.setBorder(Rectangle.BOX);
                        netValue.setPadding(8);
                        netValue.setBackgroundColor(lightGreen);
                        netValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
                        PdfPCell payDateLabel = new PdfPCell(new Phrase("Payment Date", boldSmall));
                        payDateLabel.setBorder(Rectangle.BOX);
                        payDateLabel.setPadding(8);
                        payDateLabel.setBackgroundColor(lightGreen);
                        // Last day of the month
                        java.time.LocalDate lastDay = java.time.LocalDate.of(year, month, 1).withDayOfMonth(
                                        java.time.LocalDate.of(year, month, 1).lengthOfMonth());
                        PdfPCell payDateValue = new PdfPCell(new Phrase(lastDay.toString(), normalSmall));
                        payDateValue.setBorder(Rectangle.BOX);
                        payDateValue.setPadding(8);
                        payDateValue.setBackgroundColor(lightGreen);
                        netTable.addCell(netLabel);
                        netTable.addCell(netValue);
                        netTable.addCell(payDateLabel);
                        netTable.addCell(payDateValue);
                        document.add(netTable);
                        document.close();
                        return ResponseEntity.ok()
                                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                                        "attachment; filename=payslip_" + emp.getEmpCode()
                                                                        + "_" + monthName + "_" + year + ".pdf")
                                        .contentType(MediaType.APPLICATION_PDF)
                                        .body(baos.toByteArray());
                } catch (Exception e) {
                        throw new RuntimeException("PDF generation failed: " + e.getMessage());
                }
        }

        // ── Helpers ───────────────────────────────────────────────
        private void addEmpRow(PdfPTable table, String l1, String v1, String l2, String v2,
                        Font labelFont, Font valueFont, Font normalFont, BaseColor bg) {
                PdfPCell c1 = new PdfPCell(new Phrase(l1, labelFont));
                PdfPCell c2 = new PdfPCell(new Phrase(v1, valueFont));
                PdfPCell c3 = new PdfPCell(new Phrase(l2, labelFont));
                PdfPCell c4 = new PdfPCell(new Phrase(v2, normalFont));
                for (PdfPCell c : new PdfPCell[] { c1, c2, c3, c4 }) {
                        c.setBorder(Rectangle.BOX);
                        c.setPadding(6);
                        c.setBackgroundColor(bg);
                }
                table.addCell(c1);
                table.addCell(c2);
                table.addCell(c3);
                table.addCell(c4);
        }

        private void addMainRow(PdfPTable table, String earn, String earnAmt,
                        String ded, String dedAmt, Font normalFont, Font boldFont,
                        BaseColor earnBg, BaseColor dedBg) {
                PdfPCell c1 = new PdfPCell(new Phrase(earn, normalFont));
                c1.setBorder(Rectangle.BOX);
                c1.setPadding(6);
                c1.setBackgroundColor(earnBg);
                PdfPCell c2 = new PdfPCell(new Phrase(earnAmt, boldFont));
                c2.setBorder(Rectangle.BOX);
                c2.setPadding(6);
                c2.setBackgroundColor(earnBg);
                c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
                PdfPCell c3 = new PdfPCell(new Phrase(ded, normalFont));
                c3.setBorder(Rectangle.BOX);
                c3.setPadding(6);
                c3.setBackgroundColor(dedBg);
                PdfPCell c4 = new PdfPCell(new Phrase(dedAmt, boldFont));
                c4.setBorder(Rectangle.BOX);
                c4.setPadding(6);
                c4.setBackgroundColor(dedBg);
                c4.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(c1);
                table.addCell(c2);
                table.addCell(c3);
                table.addCell(c4);
        }

        private void addTotalRow(PdfPTable table, String l1, String v1,
                        String l2, String v2, Font boldFont, BaseColor bg) {
                PdfPCell c1 = new PdfPCell(new Phrase(l1, boldFont));
                c1.setBorder(Rectangle.BOX);
                c1.setPadding(6);
                c1.setBackgroundColor(bg);
                PdfPCell c2 = new PdfPCell(new Phrase(v1, boldFont));
                c2.setBorder(Rectangle.BOX);
                c2.setPadding(6);
                c2.setBackgroundColor(bg);
                c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
                PdfPCell c3 = new PdfPCell(new Phrase(l2, boldFont));
                c3.setBorder(Rectangle.BOX);
                c3.setPadding(6);
                c3.setBackgroundColor(bg);
                PdfPCell c4 = new PdfPCell(new Phrase(v2, boldFont));
                c4.setBorder(Rectangle.BOX);
                c4.setPadding(6);
                c4.setBackgroundColor(bg);
                c4.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(c1);
                table.addCell(c2);
                table.addCell(c3);
                table.addCell(c4);
        }

        private String fmt(double v) {
                return String.format("%.2f", v);
        }

        // ✅ Inner class at the very bottom
        public static class PayslipResponse {
                public String employeeName;
                public String empCode;
                public String month;
                public int year;
                public int presentDays;
                public double perDayRate;
                public double grossSalary;
                public double earnedSalary;
                public double basicSalary;
                public double hra;
                public double transport;
                public double specialAllowance;
                public double tax;
                public double insurance;
                public double totalDeductions;
                public double netSalary;
        }
}