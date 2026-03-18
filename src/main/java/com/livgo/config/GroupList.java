package com.livgo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "run")
@Data
public class GroupList {

    private List<Long> groupList;

    private List<Long> admin;

}
