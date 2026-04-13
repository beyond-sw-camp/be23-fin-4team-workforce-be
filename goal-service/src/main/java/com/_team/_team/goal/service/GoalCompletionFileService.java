package com._team._team.goal.service;

import com._team._team.s3.S3Uploader;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
public class GoalCompletionFileService {

    private final S3Uploader s3Uploader;

    public GoalCompletionFileService(S3Uploader s3Uploader) {
        this.s3Uploader = s3Uploader;
    }

    public List<String> upload(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            urls.add(s3Uploader.upload(file, "goal-completion"));
        }
        return urls;
    }
}

