package com.react.mobile.Service;

import com.react.mobile.DTO.response.GDPRDataResponse;
import com.react.mobile.Entity.AuthUser;

public interface GDPRService {
    
    /**
     * Export tất cả dữ liệu cá nhân của user theo quy định GDPR
     * @param authUser User cần export dữ liệu
     * @return GDPRDataResponse chứa tất cả thông tin cá nhân
     */
    GDPRDataResponse exportAllData(AuthUser authUser);
    
    /**
     * Xóa tài khoản và tất cả dữ liệu liên quan (Right to be forgotten)
     * @param authUser User cần xóa
     * @param deleteType Loại xóa: "soft" (vô hiệu hóa) hoặc "hard" (xóa hoàn toàn)
     */
    void deleteAccount(AuthUser authUser, String deleteType);
    
    /**
     * Lấy lịch sử đăng nhập của user
     * @param authUser User cần xem lịch sử
     * @param limit Số lượng bản ghi tối đa
     * @return Danh sách lịch sử đăng nhập
     */
    GDPRDataResponse.UserActivityLog getActivityLogs(AuthUser authUser, Integer limit);
}
