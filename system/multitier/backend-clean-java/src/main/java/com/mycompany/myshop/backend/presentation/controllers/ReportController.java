package com.mycompany.myshop.backend.presentation.controllers;

import com.mycompany.myshop.backend.presentation.UseCaseResponder;
import com.mycompany.myshop.backend.usecases.UseCase;
import com.mycompany.myshop.backend.usecases.queries.report.ViewSalesReportRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.mycompany.myshop.backend.usecases.queries.report.ViewSalesReportResponse;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final UseCase<ViewSalesReportRequest, ViewSalesReportResponse> viewSalesReport;
    private final UseCaseResponder responder;

    public ReportController(UseCase<ViewSalesReportRequest, ViewSalesReportResponse> viewSalesReport, UseCaseResponder responder) {
        this.viewSalesReport = viewSalesReport;
        this.responder = responder;
    }

    @GetMapping("/sales")
    public ResponseEntity<Object> salesReport(@RequestParam(required = false) Integer topSkuLimit) {
        var result = viewSalesReport.execute(new ViewSalesReportRequest(topSkuLimit));
        return responder.respond(result, ResponseEntity::ok);
    }
}
