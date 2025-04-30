package tk.jandev.donutauctions.config;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class SingleValueFile {
    private final String path;
    private final File file;
    private String value;

    public SingleValueFile(String path) {
        this(path, null);
    }

    public SingleValueFile(String path, String defaultValue) {
        this.path = path;
        this.value = defaultValue;
        this.file = new File(path);
    }

    public String get() {
        return this.value;
    }

    public void setAndWrite(String value) throws IOException {
        this.value = value;
        write();
    }

    /**
     *
     * @return true if the file already existed, false if it was created
     * @throws IOException
     */
    public boolean read() throws IOException {
        if (this.file.exists()) {
            FileInputStream inputStream = new FileInputStream(this.file);

            byte[] stringData = inputStream.readAllBytes();
            this.value = new String(stringData, StandardCharsets.UTF_8);

            inputStream.close();
            return true;
        } else {
            this.file.getParentFile().mkdirs();
            this.file.createNewFile();

            return false;
        }
    }

    public void write() throws IOException {
        FileOutputStream outputStream = new FileOutputStream(this.file);

        outputStream.write(this.value.getBytes(StandardCharsets.UTF_8));
        outputStream.close();
    }

    public void writeEmpty() throws IOException {
        FileOutputStream outputStream = new FileOutputStream(this.file);

        outputStream.write(0);
        outputStream.close();
    }
}
