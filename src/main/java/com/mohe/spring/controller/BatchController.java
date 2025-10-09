package com.mohe.spring.controller;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/batch")
public class BatchController {
    private final JobLauncher jobLauncher;
    private final Job updateCrawledDataJob;

    public BatchController(JobLauncher jobLauncher, Job updateCrawledDataJob) {
        this.jobLauncher = jobLauncher;
        this.updateCrawledDataJob = updateCrawledDataJob;
    }

    @PostMapping("/update-crawled-data")
    public ResponseEntity<String> runUpdateCrawledDataJob() {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

            jobLauncher.run(updateCrawledDataJob, jobParameters);
            return ResponseEntity.ok("Batch job started successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
}
