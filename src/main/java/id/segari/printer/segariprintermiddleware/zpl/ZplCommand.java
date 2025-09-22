package id.segari.printer.segariprintermiddleware.zpl;

/**
 * ZPL (Zebra Programming Language) Command Builder
 * Provides methods to build ZPL commands for printing
 */
public class ZplCommand {

    private final StringBuilder zpl = new StringBuilder();

    public ZplCommand() {
        // Start ZPL format
        zpl.append("^XA");
    }

    /**
     * Add text field
     */
    public ZplCommand addText(int x, int y, String text, int fontSize) {
        zpl.append("^FO").append(x).append(",").append(y)
           .append("^A0N,").append(fontSize).append(",").append(fontSize)
           .append("^FD").append(text).append("^FS");
        return this;
    }

    /**
     * Add text with font selection
     */
    public ZplCommand addText(int x, int y, String text, String font, int width, int height) {
        zpl.append("^FO").append(x).append(",").append(y)
           .append("^A").append(font).append("N,").append(height).append(",").append(width)
           .append("^FD").append(text).append("^FS");
        return this;
    }

    /**
     * Add QR Code
     */
    public ZplCommand addQRCode(int x, int y, String data, int size) {
        zpl.append("^FO").append(x).append(",").append(y)
           .append("^BQN,2,").append(size)
           .append("^FDMA,").append(data).append("^FS");
        return this;
    }

    /**
     * Add Code 128 Barcode
     */
    public ZplCommand addBarcode128(int x, int y, String data, int height, int width) {
        zpl.append("^FO").append(x).append(",").append(y)
           .append("^BCN,").append(height).append(",Y,N,N")
           .append("^FD").append(data).append("^FS");
        return this;
    }

    /**
     * Add horizontal line
     */
    public ZplCommand addLine(int x, int y, int width, int thickness) {
        zpl.append("^FO").append(x).append(",").append(y)
           .append("^GB").append(width).append(",").append(thickness).append(",").append(thickness)
           .append("^FS");
        return this;
    }

    /**
     * Add rectangle/box
     */
    public ZplCommand addBox(int x, int y, int width, int height, int thickness) {
        zpl.append("^FO").append(x).append(",").append(y)
           .append("^GB").append(width).append(",").append(height).append(",").append(thickness)
           .append("^FS");
        return this;
    }

    /**
     * Set label home position
     */
    public ZplCommand setLabelHome(int x, int y) {
        zpl.append("^LH").append(x).append(",").append(y);
        return this;
    }

    /**
     * Set print width
     */
    public ZplCommand setPrintWidth(int width) {
        zpl.append("^PW").append(width);
        return this;
    }

    /**
     * Set label length
     */
    public ZplCommand setLabelLength(int length) {
        zpl.append("^LL").append(length);
        return this;
    }

    /**
     * Set print quantity
     */
    public ZplCommand setPrintQuantity(int quantity) {
        zpl.append("^PQ").append(quantity);
        return this;
    }

    /**
     * Set print speed
     */
    public ZplCommand setPrintSpeed(int speed) {
        zpl.append("^PR").append(speed);
        return this;
    }

    /**
     * Set print darkness
     */
    public ZplCommand setPrintDarkness(int darkness) {
        zpl.append("^MD").append(darkness);
        return this;
    }

    /**
     * Add raw ZPL command
     */
    public ZplCommand addRaw(String command) {
        zpl.append(command);
        return this;
    }

    /**
     * Build final ZPL command
     */
    public String build() {
        // End ZPL format
        return zpl.toString() + "^XZ";
    }

    /**
     * Create a simple text label
     */
    public static String createTextLabel(String text, int x, int y, int fontSize) {
        return new ZplCommand()
                .addText(x, y, text, fontSize)
                .build();
    }

    /**
     * Create a QR code label
     */
    public static String createQRLabel(String data, int x, int y, int size) {
        return new ZplCommand()
                .addQRCode(x, y, data, size)
                .build();
    }

    /**
     * Create barcode label
     */
    public static String createBarcodeLabel(String data, int x, int y, int height) {
        return new ZplCommand()
                .addBarcode128(x, y, data, height, 2)
                .build();
    }
}