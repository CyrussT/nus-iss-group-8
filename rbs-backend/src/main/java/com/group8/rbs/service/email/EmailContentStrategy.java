package com.group8.rbs.service.email;

import java.util.Map;

public interface EmailContentStrategy {
    String buildSubject(Map<String, Object> params);

    String buildBody(Map<String, Object> params);
}
