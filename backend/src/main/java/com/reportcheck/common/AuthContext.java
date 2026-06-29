package com.reportcheck.common;

public final class AuthContext {

    private static final ThreadLocal<AuthInfo> HOLDER = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void set(AuthInfo authInfo) {
        HOLDER.set(authInfo);
    }

    public static AuthInfo get() {
        return HOLDER.get();
    }

    public static Long currentUserId() {
        AuthInfo authInfo = get();
        return authInfo == null ? null : authInfo.getUserId();
    }

    public static String currentRoleCode() {
        AuthInfo authInfo = get();
        return authInfo == null ? null : authInfo.getRoleCode();
    }

    public static boolean hasRole(String roleCode) {
        AuthInfo authInfo = get();
        return authInfo != null && roleCode.equals(authInfo.getRoleCode());
    }

    public static void clear() {
        HOLDER.remove();
    }
}
