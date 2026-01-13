package com.smartgrade.smartgrade_backend.service;

import com.smartgrade.smartgrade_backend.entity.QuizEntity;
import com.smartgrade.smartgrade_backend.entity.SheetEntity;
import com.smartgrade.smartgrade_backend.repository.SheetRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SheetService {

    private final SheetRepository sheetRepository;

    public SheetService(SheetRepository sheetRepository) {
        this.sheetRepository = sheetRepository;
    }

    public List<SheetEntity> createSheets(QuizEntity quiz, int layoutVersion, int count) {
        if (count <= 0) count = 1;

        List<SheetEntity> created = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            SheetEntity s = new SheetEntity(quiz, layoutVersion);
            created.add(sheetRepository.save(s)); // need id for QR
        }
        return created;
    }
}
