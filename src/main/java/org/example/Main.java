package org.example;

import com.jsoniter.JsonIterator;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.time.DateUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Main {

    static String THREE_DIGIT_FILE_NAME = "./3-end-digit-xsmb-from-26-08-2020.txt";

    static String TWO_DIGIT_FILE_NAME = "./2-end-digit-xsmb-from-26-08-2020.txt";

    public static void main(String[] args) throws Exception {
        String lastRun = new Scanner(new File("./last-run.txt")).next().trim();
        getAndWriteDataFromDate(lastRun);
        System.out.println("Done !");
    }

    private static void getAndWriteDataFromDate(String startDate) throws ParseException {
        OkHttpClient okHttpClient = new OkHttpClient();
        Date date = new SimpleDateFormat("yyyy-MM-dd").parse(startDate);
        date = DateUtils.addDays(date, 1);

        while (true) {
            try {
                Integer resultOfDate = getResultOfDate(okHttpClient, date);
                writeToExistedFile(String.format("%3d\r\n", resultOfDate % 1000), "./3-end-digit-xsmb-from-26-08-2020.txt");
                replaceFileContent(new SimpleDateFormat("yyyy-MM-dd").format(date), "./last-run.txt");
                handle(date);
            } catch (Exception ignore) {
                break;
            }
            date = DateUtils.addDays(date, 1);
        }
    }

    private static Integer getResultOfDate(OkHttpClient client, Date date) throws IOException {
        try {
            Request request = new Request.Builder()
                    .url("https://api.tinhtienso.com/api/result?date=" + new SimpleDateFormat("yyyy-MM-dd").format(date))
                    .build();

            Call call = client.newCall(request);
            Response response = call.execute();
            String responseBody = response.body().string();
            String resultStr = JsonIterator.deserialize(responseBody).toString("data", "north", 0, "result", "special", 0);
            return Integer.parseInt(resultStr);
        } catch (Exception e) {
            throw e;
        }
    }

    private static void writeToExistedFile(String data, String filePath) {
        try {
            Files.write(Paths.get(filePath), data.getBytes(), StandardOpenOption.APPEND);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void replaceFileContent(String data, String filePath) {
        try {
            Files.write(Paths.get(filePath), data.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void merge3DigitTo2Digit() throws FileNotFoundException {
        Scanner scanner = new Scanner(new File(THREE_DIGIT_FILE_NAME));
        scanner.useDelimiter("\n");
        while(scanner.hasNext()) {
            String value = scanner.next().trim();
            Integer intValue = Integer.parseInt(value);
            writeToExistedFile(intValue%100 + "\r\n", TWO_DIGIT_FILE_NAME);
        }
    }

    private static void handle(Date date) throws FileNotFoundException {
        // Handle for 3-digits
        Map<Integer, Integer> threeDigitHashMap = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            threeDigitHashMap.put(i, 0);
        }
        Integer lastNumber = -1;
        Scanner scanner = new Scanner(new File(THREE_DIGIT_FILE_NAME));
        scanner.useDelimiter("\n");
        while (scanner.hasNext()) {
            String value = scanner.next().trim();
            lastNumber = Integer.parseInt(value);
            threeDigitHashMap.put(lastNumber, threeDigitHashMap.get(lastNumber) + 1);
        }
        List<Map.Entry<Integer, Integer>> entryList = new LinkedList<>(threeDigitHashMap.entrySet());
        Collections.sort(entryList, (Map.Entry.comparingByValue()));

        writeToExistedFile(String.format("%s result is %03d, result has been occurred: %d times before\n",
                new SimpleDateFormat("dd/MM/yyyy").format(date), lastNumber,
                threeDigitHashMap.get(lastNumber) - 1), "./result.md");

        writeToExistedFile(String.valueOf(threeDigitHashMap.get(lastNumber) - 1) + "\r\n", "./occurrent.txt");

        writeToExistedFile("Top 10 most frequently: \n", "./result.md");
        for (int i=0; i<10; i++) {
            writeToExistedFile(String.format("%03d %s\n", entryList.get(entryList.size()-1-i).getKey(), entryList.get(entryList.size()-1-i).getValue()), "./result.md");
            if (entryList.get(entryList.size()-1-i).getKey().equals(lastNumber)) {
                writeToExistedFile("~BINGO~", "./result.md");
            }
        }
        writeToExistedFile("All not exist: \n", "./result.md");
        List<Integer> notExistNumber = new ArrayList<>();
        int i=0;
        while (entryList.get(i).getValue() == 0) {
            writeToExistedFile(String.format("%03d ", entryList.get(i).getKey()), "./result.md");
            notExistNumber.add(entryList.get(i).getKey());
            if (entryList.get(i).getKey().equals(lastNumber)) {
                writeToExistedFile("~BINGO~\n", "./result.md");
            }
            i++;
        }
        writeToExistedFile("\nRandom 30 not exist: \n", "./result.md");
        int randomCount = 0;
        int randomIndex;
        while (randomCount < 30) {
            randomIndex = (int) (Math.random() * notExistNumber.size());
            writeToExistedFile(String.format("%03d\n", notExistNumber.get(randomIndex)), "./result.md");
            if (notExistNumber.get(randomIndex).equals(lastNumber)) {
                writeToExistedFile("~BINGO~\n", "./result.md");
            }
            notExistNumber.remove(randomIndex);
            randomCount++;
        }
    }
}