package AlertController;
import java.time.LocalDateTime;
public class PollutionAlert {
    private final String district, severity;
    private final int aqi;
    private final LocalDateTime timestamp;
    public PollutionAlert(String district,int aqi,String severity) {
        this(district,aqi,severity,LocalDateTime.now());
    }
    public PollutionAlert(String district,int aqi,String severity,LocalDateTime timestamp) {
        this.district=district;
        this.aqi=aqi;
        this.severity=severity;
        this.timestamp=timestamp;
    }
    public String getDistrict() {
        return district;
    }
    public int getAqi() {
        return aqi;
    }
    public String getSeverity() {
        return severity;
    }
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    public String toString() {
        return timestamp+" | "+district+" | AQI: "+aqi+" | "+severity;
    }
}
