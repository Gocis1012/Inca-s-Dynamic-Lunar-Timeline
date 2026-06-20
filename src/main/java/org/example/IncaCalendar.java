import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class IncaCalendar {
    
    // ใช้ Record เก็บโครงสร้าง วัน-เดือน-ปี (ต้องใช้ Java 14+)
    record IncaDate(int year, int month, int day) {}

    public static void main(String[] args) {
        String calendarFile = "Q1_Sample_Calendar.csv";
        String censusFile = "Q1_Sample_CivilRegistration.csv";

        Map<IncaDate, Integer> dateToSeq = new HashMap<>();
        Map<Integer, Integer> monthsInYear = new HashMap<>();

        // ==========================================
        // Phase 1: อ่านไฟล์ปฏิทินและสร้างข้อมูล (Precompute)
        // ==========================================
        try (BufferedReader br = new BufferedReader(new FileReader(calendarFile))) {
            String line = br.readLine(); // ข้ามบรรทัด Header (Seq,Clear?,Full ?,#Scutes)
            
            int y = 1, m = 1, d = 1;
            int monthsThisYear = 12;

            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 4) continue;
                
                int seq = Integer.parseInt(parts[0].trim());
                boolean clearSky = Boolean.parseBoolean(parts[1].trim());
                boolean fullMoon = Boolean.parseBoolean(parts[2].trim());
                int scutes = Integer.parseInt(parts[3].trim());

                dateToSeq.put(new IncaDate(y, m, d), seq);

                // เช็คเงื่อนไขเดือน 13 ในเช้าวันแรกของเดือน 12
                if (m == 12 && d == 1) {
                    monthsThisYear = (scutes == 13) ? 13 : 12;
                }
                monthsInYear.put(y, monthsThisYear);

                // ตรวจสอบการขึ้นเดือนใหม่
                boolean isNewMonth = false;
                if (d >= 28 && d <= 30) {
                    if (fullMoon && clearSky) isNewMonth = true;
                } else if (d == 31) {
                    isNewMonth = true; // วันที่ 31 ต้องขึ้นเดือนใหม่เสมอ
                }

                // อัปเดตตัวแปร Y, M, D
                if (isNewMonth) {
                    d = 1;
                    if (m == monthsThisYear) {
                        m = 1;
                        y++;
                        monthsThisYear = 12;
                    } else {
                        m++;
                    }
                } else {
                    d++;
                }
            }
        } catch (IOException e) {
            System.err.println("เกิดข้อผิดพลาดในการอ่านไฟล์ Calendar: " + e.getMessage());
        }

        // ==========================================
        // Phase 2: อ่านไฟล์ข้อมูลประชากรและคำนวณอายุ
        // ==========================================
        try (BufferedReader br = new BufferedReader(new FileReader(censusFile))) {
            String line = br.readLine(); // ข้ามบรรทัด Header (id,birth_dt,dead_dt)
            
            System.out.printf("%-10s %-12s %-10s%n", "ID", "Age in Days", "Age (Y/M)");
            System.out.println("----------------------------------------");
            
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 3) continue;

                String id = parts[0].trim();
                String[] dobParts = parts[1].trim().split("-");
                String[] dodParts = parts[2].trim().split("-");

                int d_b = Integer.parseInt(dobParts[0]);
                int m_b = Integer.parseInt(dobParts[1]);
                int y_b = Integer.parseInt(dobParts[2]);

                int d_d = Integer.parseInt(dodParts[0]);
                int m_d = Integer.parseInt(dodParts[1]);
                int y_d = Integer.parseInt(dodParts[2]);

                Integer seq_b = dateToSeq.get(new IncaDate(y_b, m_b, d_b));
                Integer seq_d = dateToSeq.get(new IncaDate(y_d, m_d, d_d));

                // เผื่อกรณีข้อมูลวันเกิด/ตายไม่พบในปฏิทินที่สร้างไว้
                if (seq_b == null || seq_d == null) {
                    System.out.printf("%-10s %-12s %-10s%n", id, "N/A", "N/A");
                    continue;
                }

                // 1. คำนวณ Age in Days
                int ageDays = seq_d - seq_b;
                
                // 2. คำนวณ Age (Y/M)
                int yDiff = y_d - y_b;
                int ageY = (m_d > m_b || (m_d == m_b && d_d >= d_b)) ? yDiff : yDiff - 1;

                int ageM = 0;
                int currY = y_b + ageY;
                int currM = m_b;

                while (currY != y_d || currM != m_d) {
                    currM++;
                    int maxM = monthsInYear.getOrDefault(currY, 12);
                    if (currM > maxM) {
                        currM = 1;
                        currY++;
                    }
                    ageM++;
                }

                // หักเศษเดือนออกหากวันตายยังไม่ถึงวันเกิด
                if (d_d < d_b) {
                    ageM--;
                }

                // Print ผลลัพธ์ออกหน้าจอ
                System.out.printf("%-10s %-12d %-10s%n", id, ageDays, ageY + "/" + ageM);
            }
        } catch (IOException e) {
            System.err.println("เกิดข้อผิดพลาดในการอ่านไฟล์ Census: " + e.getMessage());
        }
    }
}