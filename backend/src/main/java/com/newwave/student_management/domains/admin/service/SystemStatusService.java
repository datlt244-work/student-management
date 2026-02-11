package com.newwave.student_management.domains.admin.service;

import com.newwave.student_management.domains.admin.dto.ComponentStatusDto;
import com.newwave.student_management.domains.admin.dto.SystemHealthResponse;

public interface SystemStatusService {

    SystemHealthResponse overall();

    ComponentStatusDto checkApi();

    ComponentStatusDto checkDatabase();

    ComponentStatusDto checkRedis();

    ComponentStatusDto checkMail();

    ComponentStatusDto checkMinIO();

    ComponentStatusDto checkNginx();

    ComponentStatusDto checkFrontend();
}


