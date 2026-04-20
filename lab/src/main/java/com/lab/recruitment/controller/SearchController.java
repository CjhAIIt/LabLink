package com.lab.recruitment.controller;

import com.lab.recruitment.entity.User;
import com.lab.recruitment.service.SearchService;
import com.lab.recruitment.support.CurrentUserAccessor;
import com.lab.recruitment.utils.Result;
import com.lab.recruitment.vo.SearchResultItemVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/search")
public class SearchController {

    @Autowired
    private SearchService searchService;

    @Autowired
    private CurrentUserAccessor currentUserAccessor;

    @GetMapping("/global")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Map<String, Object>> globalSearch(@RequestParam String keyword,
                                                    @RequestParam(required = false) Integer limit,
                                                    @RequestParam(required = false) Long collegeId,
                                                    @RequestParam(required = false) Long labId) {
        User currentUser = currentUserAccessor.getCurrentUser();
        return Result.apiSuccess(searchService.globalSearch(keyword, limit, collegeId, labId, currentUser));
    }

    @GetMapping("/labs")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<List<SearchResultItemVO>> searchLabs(@RequestParam String keyword,
                                                       @RequestParam(required = false) Integer limit,
                                                       @RequestParam(required = false) Long collegeId,
                                                       @RequestParam(required = false) Long labId) {
        User currentUser = currentUserAccessor.getCurrentUser();
        return Result.apiSuccess(searchService.searchLabs(keyword, limit, collegeId, labId, currentUser));
    }

    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<List<SearchResultItemVO>> searchUsers(@RequestParam String keyword,
                                                        @RequestParam(required = false) Integer limit,
                                                        @RequestParam(required = false) Long collegeId,
                                                        @RequestParam(required = false) Long labId) {
        User currentUser = currentUserAccessor.getCurrentUser();
        return Result.apiSuccess(searchService.searchUsers(keyword, limit, collegeId, labId, currentUser));
    }

    @GetMapping("/profiles")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<List<SearchResultItemVO>> searchProfiles(@RequestParam String keyword,
                                                           @RequestParam(required = false) Integer limit,
                                                           @RequestParam(required = false) Long collegeId,
                                                           @RequestParam(required = false) Long labId) {
        User currentUser = currentUserAccessor.getCurrentUser();
        return Result.apiSuccess(searchService.searchProfiles(keyword, limit, collegeId, labId, currentUser));
    }

    @GetMapping("/devices")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<List<SearchResultItemVO>> searchDevices(@RequestParam String keyword,
                                                          @RequestParam(required = false) Integer limit,
                                                          @RequestParam(required = false) Long collegeId,
                                                          @RequestParam(required = false) Long labId) {
        User currentUser = currentUserAccessor.getCurrentUser();
        return Result.apiSuccess(searchService.searchDevices(keyword, limit, collegeId, labId, currentUser));
    }

    @GetMapping("/files")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<List<SearchResultItemVO>> searchFiles(@RequestParam String keyword,
                                                        @RequestParam(required = false) Integer limit,
                                                        @RequestParam(required = false) Long collegeId,
                                                        @RequestParam(required = false) Long labId) {
        User currentUser = currentUserAccessor.getCurrentUser();
        return Result.apiSuccess(searchService.searchFiles(keyword, limit, collegeId, labId, currentUser));
    }
}
