package com.mohe.spring.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Weather information response DTO
 */
@Schema(description = "날씨 정보 응답")
public class WeatherResponse {

    @Schema(description = "섭씨 온도", example = "23.5")
    private double temperatureCelsius;

    @Schema(description = "화씨 온도", example = "74.3")
    private double temperatureFahrenheit;

    @Schema(description = "날씨 상태 코드 (clear, rain, snow, cloudy 등)", example = "clear")
    private String conditionCode;

    @Schema(description = "날씨 상태 설명 (한글)", example = "맑음")
    private String conditionText;

    @Schema(description = "습도 (%)", example = "65")
    private int humidity;

    @Schema(description = "풍속 (km/h)", example = "12.5")
    private double windSpeedKmh;

    @Schema(description = "시간대 (morning, afternoon, evening, night)", example = "afternoon")
    private String daypart;

    @Schema(description = "날씨 조회 좌표 - 위도", example = "37.5665")
    private double latitude;

    @Schema(description = "날씨 조회 좌표 - 경도", example = "126.9780")
    private double longitude;

    @Schema(description = "날씨 데이터 제공자 (KMA, OpenMeteo 등)", example = "KMA")
    private String provider;

    // Constructors
    public WeatherResponse() {}

    public WeatherResponse(double temperatureCelsius, double temperatureFahrenheit,
                          String conditionCode, String conditionText,
                          int humidity, double windSpeedKmh, String daypart,
                          double latitude, double longitude, String provider) {
        this.temperatureCelsius = temperatureCelsius;
        this.temperatureFahrenheit = temperatureFahrenheit;
        this.conditionCode = conditionCode;
        this.conditionText = conditionText;
        this.humidity = humidity;
        this.windSpeedKmh = windSpeedKmh;
        this.daypart = daypart;
        this.latitude = latitude;
        this.longitude = longitude;
        this.provider = provider;
    }

    // Getters and setters
    public double getTemperatureCelsius() { return temperatureCelsius; }
    public void setTemperatureCelsius(double temperatureCelsius) { this.temperatureCelsius = temperatureCelsius; }

    public double getTemperatureFahrenheit() { return temperatureFahrenheit; }
    public void setTemperatureFahrenheit(double temperatureFahrenheit) { this.temperatureFahrenheit = temperatureFahrenheit; }

    public String getConditionCode() { return conditionCode; }
    public void setConditionCode(String conditionCode) { this.conditionCode = conditionCode; }

    public String getConditionText() { return conditionText; }
    public void setConditionText(String conditionText) { this.conditionText = conditionText; }

    public int getHumidity() { return humidity; }
    public void setHumidity(int humidity) { this.humidity = humidity; }

    public double getWindSpeedKmh() { return windSpeedKmh; }
    public void setWindSpeedKmh(double windSpeedKmh) { this.windSpeedKmh = windSpeedKmh; }

    public String getDaypart() { return daypart; }
    public void setDaypart(String daypart) { this.daypart = daypart; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
}
