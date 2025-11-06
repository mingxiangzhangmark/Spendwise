package com.example.backend.controller;

import com.example.backend.dto.*;
import com.example.backend.model.ExpenseRecord;
import com.example.backend.model.FeatureSnapshot;
import com.example.backend.model.RecurringExpenseSchedule;
import com.example.backend.model.User;
import com.example.backend.repository.ExpenseRecordRepository;
import com.example.backend.repository.FeatureSnapshotRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.ExpenseRecordService;
import com.example.backend.service.RecurringExpenseService;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/records")
public class ExpenseRecordController {

    private final ExpenseRecordService recordService;
    private final RecurringExpenseService recurringExpenseService;
    private final ExpenseRecordRepository expenseRecordRepository;
    private final FeatureSnapshotRepository  featureSnapshotRepository;
    private final UserRepository userRepository;

    public ExpenseRecordController(ExpenseRecordService recordService, RecurringExpenseService recurringExpenseService,
                                   ExpenseRecordRepository expenseRecordRepository, UserRepository userRepository,
                                   FeatureSnapshotRepository featureSnapshotRepository) {
        this.recordService = recordService;
        this.recurringExpenseService = recurringExpenseService;
        this.expenseRecordRepository = expenseRecordRepository;
        this.featureSnapshotRepository = featureSnapshotRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/month")
    public SnapshotDTO getSnapshot(HttpSession session,
                                   @RequestParam(required = false) String month) {
//{
//    "id": 1,
//    "totalsByCategoryJson": "[{\"amount\":12.50,\"catId\":9,\"catName\":\"Food\",\"pct\":0.7962},{\"amount\":3.20,\"catId\":10,\"catName\":\"Transport\",\"pct\":0.2038}]",
//    "totalSpending": 15.70,
//    "currency": "AUD"
//}
        UserDTO userDTO = (UserDTO) session.getAttribute("USER");
        Integer userId = userDTO.getId();
        User user =  userRepository.getReferenceById(userId);
        FeatureSnapshot fs = featureSnapshotRepository
                .findByUserAndMonth(user, month)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "snapshot not found"));

        return SnapshotDTO.toDTO(fs);
    }


    @GetMapping
    public ResponseEntity<List<ExpenseRecordDTO>> getRecords(HttpSession session) {
        // [
        //    {
        //        "expenseId": 3,
        //        "user": {
        //            "id": 6,
        //            "username": "bob"
        //        },
        //        "category": {
        //            "id": 9,
        //            "name": "Food"
        //        },
        //        "amount": 12.50,
        //        "currency": "USD",
        //        "expenseDate": "2025-09-19",
        //        "description": "Lunch at cafe",
        //        "isRecurring": null,
        //        "recurringScheduleId": null,
        //        "paymentMethod": null
        //    },
        //        ...
        // ]
        UserDTO user = (UserDTO) session.getAttribute("USER");
        Integer userId = user.getId();
        List<ExpenseRecordDTO> dtoList = recordService.getRecordsForUser(userId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtoList);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<ExpenseRecordDTO>> search(
            HttpSession session,
            @RequestParam(required = false) String from,        // YYYY-MM-DD（含当天）
            @RequestParam(required = false) String to,          // YYYY-MM-DD（含当天）
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String q,           // 关键字：description/notes 模糊匹配
            @RequestParam(required = false) Boolean recurring,  // 只看周期/非周期
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "expenseDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        //  {
        //    "content": [
        //        {
        //            "expenseId": 23,
        //            "user": {
        //                "id": 6,
        //                "username": "bob"
        //            },
        //            "category": {
        //                "id": 9,
        //                "name": "Food"
        //            },
        //            "amount": 12.50,
        //            "currency": "USD",
        //            "expenseDate": "2025-10-13",
        //            "description": "",
        //            "isRecurring": false,
        //            "recurringScheduleId": null,
        //            "paymentMethod": null
        //        },
        //        {
        //            "expenseId": 24,
        //            "user": {
        //                "id": 6,
        //                "username": "bob"
        //            },
        //            "category": {
        //                "id": 10,
        //                "name": "Transport"
        //            },
        //            "amount": 3.20,
        //            "currency": "USD",
        //            "expenseDate": "2025-10-13",
        //            "description": "",
        //            "isRecurring": false,
        //            "recurringScheduleId": null,
        //            "paymentMethod": null
        //        }
        //    ],
        //    "pageable": {
        //        "pageNumber": 0,
        //        "pageSize": 10,
        //        "sort": {
        //            "empty": false,
        //            "sorted": true,
        //            "unsorted": false
        //        },
        //        "offset": 0,
        //        "paged": true,
        //        "unpaged": false
        //    },
        //    "last": true,
        //    "totalPages": 1,
        //    "totalElements": 2,
        //    "first": true,
        //    "numberOfElements": 2,
        //    "size": 10,
        //    "number": 0,
        //    "sort": {
        //        "empty": false,
        //        "sorted": true,
        //        "unsorted": false
        //    },
        //    "empty": false
        //}
        Integer userId = ((UserDTO) session.getAttribute("USER")).getId();

        LocalDate fromDate = (from == null || from.isBlank()) ? null : LocalDate.parse(from);
        LocalDate toDate   = (to   == null || to.isBlank())   ? null : LocalDate.parse(to);

        Sort.Direction dir = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by(dir, sortBy));

        Page<ExpenseRecord> entities = recordService.search(
                userId, fromDate, toDate, categoryId, q, recurring, pageable);

        Page<ExpenseRecordDTO> dtoPage = entities.map(this::toDTO);
        return ResponseEntity.ok(dtoPage);
    }

    @PostMapping
    public ResponseEntity<ExpenseRecordDTO> createRecord(@RequestBody ExpenseRecord recordData,
                                                         @RequestParam(required = false) String frequency,
                                                         HttpSession session) {
        // {
        //    "expenseId": 8,
        //    "user": {
        //        "id": 6,
        //        "username": "bob"
        //    },
        //    "category": {
        //        "id": 9,
        //        "name": "Food"
        //    },
        //    "amount": 50,
        //    "currency": "USD",
        //    "expenseDate": "2025-09-19",
        //    "description": null,
        //    "isRecurring": null,
        //    "recurringScheduleId": null,
        //    "paymentMethod": null
        //}
        UserDTO user = (UserDTO) session.getAttribute("USER");
        Integer userId = user.getId();
        ExpenseRecord created = recordService.createRecord(userId, recordData);
        if (recordData.getIsRecurring() && frequency != null && !frequency.isBlank()) {
            RecurringExpenseSchedule.Frequency freq =
                    RecurringExpenseSchedule.Frequency.valueOf(frequency.toUpperCase());
            recurringExpenseService.onManualExpenseSaved(created, freq);
        }
        ExpenseRecordDTO dto = toDTO(created);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExpenseRecordDTO> updateRecord(
            @PathVariable Integer id,
            @RequestBody ExpenseRecord updatedData,
            @RequestParam(required = false) String frequency,
            HttpSession session) {
        // {
        //    "expenseId": 7,
        //    "user": {
        //        "id": 6,
        //        "username": "bob"
        //    },
        //    "category": {
        //        "id": 9,
        //        "name": "Food"
        //    },
        //    "amount": 320,
        //    "currency": "USD",
        //    "expenseDate": "2025-09-19",
        //    "description": null,
        //    "isRecurring": null,
        //    "recurringScheduleId": null,
        //    "paymentMethod": null
        //}
        UserDTO user = (UserDTO) session.getAttribute("USER");
        Integer userId = user.getId();

        ExpenseRecord before = expenseRecordRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found"));

        boolean oldRecurring = Boolean.TRUE.equals(before.getIsRecurring());
        RecurringExpenseSchedule oldSchedule = before.getRecurringSchedule();

        ExpenseRecord saved = recordService.updateRecord(userId, id, updatedData);

        // 若前端没传 isRecurring，则不调整周期逻辑
        Boolean newRecurring = updatedData.getIsRecurring();
        if (newRecurring == null) {
            return ResponseEntity.ok(toDTO(saved));
        }

        // false -> true：要求 frequency，调用建/绑/推进
        if (!oldRecurring && newRecurring) {
            if (frequency == null || frequency.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "frequency required when isRecurring=true");
            }
            RecurringExpenseSchedule.Frequency newFreq =
                    RecurringExpenseSchedule.Frequency.valueOf(frequency.toUpperCase());
            recurringExpenseService.onManualExpenseSaved(saved, newFreq);
            return ResponseEntity.ok(toDTO(saved));
        }

        // true -> false：取消整个计划，保留当前记录
        if (oldRecurring && !newRecurring) {
            if (oldSchedule != null) {
                recurringExpenseService.cancelSchedule(oldSchedule.getId());
            }
            saved.setIsRecurring(false);
            saved.setRecurringSchedule(null);
            expenseRecordRepository.save(saved);
            return ResponseEntity.ok(toDTO(saved));
        }

        // true -> true：可能变更频率
        if (oldRecurring) {
            if (frequency != null && !frequency.isBlank()) {
                RecurringExpenseSchedule.Frequency newFreq =
                        RecurringExpenseSchedule.Frequency.valueOf(frequency.toUpperCase());
                RecurringExpenseSchedule.Frequency oldFreq =
                        (oldSchedule != null ? oldSchedule.getFrequency() : null);

                if (oldSchedule == null) {
                    // 宽容处理：标记为 recurring 但没有 schedule，则补建
                    recurringExpenseService.onManualExpenseSaved(saved, newFreq);
                } else if (oldFreq != newFreq) {
                    // 频率变更：取消旧计划并以当前记录为锚新建
                    recurringExpenseService.cancelSchedule(oldSchedule.getId());
                    recurringExpenseService.onManualExpenseSaved(saved, newFreq);
                }
            }
            return ResponseEntity.ok(toDTO(saved));
        }
        return ResponseEntity.ok(toDTO(saved));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String,String>> deleteRecord(@PathVariable Integer id,
                                                           @RequestParam(defaultValue = "false") boolean cancelRecurring,
                                                           HttpSession session) {
        //{
        //    "message": "Record deleted successfully"
        //}
        UserDTO user = (UserDTO) session.getAttribute("USER");
        Integer userId = user.getId();
        ExpenseRecord rec = expenseRecordRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found"));
        if (!rec.getUser().getUser_id().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your record");
        }

        if (cancelRecurring && rec.getRecurringSchedule() != null) {
            recurringExpenseService.cancelSchedule(rec.getRecurringSchedule().getId());
        }
        recordService.deleteRecord(userId, id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Record deleted successfully");
        return ResponseEntity.ok(response);
    }

    private ExpenseRecordDTO toDTO(ExpenseRecord record) {
        ExpenseRecordDTO dto = new ExpenseRecordDTO();
        dto.setExpenseId(Long.valueOf(record.getExpenseId()));

        // User
        UserDTO userDTO = new UserDTO();
        userDTO.setId(record.getUser().getUser_id());
        userDTO.setUsername(record.getUser().getUsername());
        dto.setUser(userDTO);

        // Category
        CategoryDTO categoryDTO = new CategoryDTO();
        categoryDTO.setId(Long.valueOf(record.getCategory().getCategoryId()));
        categoryDTO.setName(record.getCategory().getCategoryName());
        dto.setCategory(categoryDTO);

        dto.setAmount(record.getAmount());
        dto.setCurrency(record.getCurrency());
        dto.setExpenseDate(record.getExpenseDate());
        dto.setDescription(record.getDescription());

        // Recurring
        dto.setIsRecurring(record.getIsRecurring());
        if(record.getIsRecurring() == null) {
            dto.setIsRecurring(false);
        }
        if(record.getIsRecurring() && record.getRecurringSchedule() != null){
            dto.setRecurringScheduleId(record.getRecurringSchedule().getId());
        }
        return dto;
    }

    // === Reports ===

    // Weekly
    @GetMapping("/reports/weekly")
    public ResponseEntity<List<ExpenseReportDTO>> getWeeklyReport(
            @RequestParam Integer year,
            @RequestParam Integer week,
            HttpSession session) {
        UserDTO user = (UserDTO) session.getAttribute("USER");
        Integer userId = user.getId();
        return ResponseEntity.ok(recordService.getWeeklyReport(userId, year, week));
    }

    // Monthly
    @GetMapping("/reports/monthly")
    public ResponseEntity<List<ExpenseReportDTO>> getMonthlyReport(
            @RequestParam Integer year,
            @RequestParam Integer month,
            HttpSession session) {
        UserDTO user = (UserDTO) session.getAttribute("USER");
        Integer userId = user.getId();
        return ResponseEntity.ok(recordService.getMonthlyReport(userId, year, month));
    }

    // Yearly
    @GetMapping("/reports/yearly")
    public ResponseEntity<List<ExpenseReportDTO>> getYearlyReport(
            @RequestParam Integer year,
            HttpSession session) {
        UserDTO user = (UserDTO) session.getAttribute("USER");
        Integer userId = user.getId();
        return ResponseEntity.ok(recordService.getYearlyReport(userId, year));
    }

}
