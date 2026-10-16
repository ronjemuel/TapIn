package tapin.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class Csv {
    private Csv() {}
    public static String cell(Object value) {
        String text=Objects.toString(value, "");
        if(text.matches("(?s)^[=+@\\-\\t\\r].*")) text="'"+text;
        return "\""+text.replace("\"","\"\"")+"\"";
    }
    public static void write(Path path,List<String> headers,List<List<Object>> rows) throws IOException {
        try(BufferedWriter out=Files.newBufferedWriter(path,StandardCharsets.UTF_8)) {
            out.write('\uFEFF');
            out.write(String.join(",",headers.stream().map(Csv::cell).toList()));out.write("\r\n");
            for(List<Object> row:rows) {out.write(String.join(",",row.stream().map(Csv::cell).toList()));out.write("\r\n");}
        }
    }
}
