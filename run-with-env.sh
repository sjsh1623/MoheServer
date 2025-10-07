#!/bin/bash

# ===========================================
# Mohe Spring Boot - 환경변수 로딩 스크립트
# ===========================================
# .env 파일을 읽어서 환경변수로 설정한 후 Spring Boot 실행

set -a  # 모든 변수를 자동으로 export
source .env
set +a

# Java 21 설정
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home

echo "🚀 Starting Mohe Spring Boot with environment variables from .env"
echo "📍 NAVER_CLIENT_ID: ${NAVER_CLIENT_ID:0:10}..."
echo "📍 NAVER_CLIENT_SECRET: ${NAVER_CLIENT_SECRET:0:5}..."

# Gradle로 Spring Boot 실행
./gradlew bootRun
