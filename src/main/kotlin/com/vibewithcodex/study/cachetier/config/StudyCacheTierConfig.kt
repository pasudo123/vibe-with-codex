package com.vibewithcodex.study.cachetier.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(StudyCacheTierProperties::class)
class StudyCacheTierConfig
