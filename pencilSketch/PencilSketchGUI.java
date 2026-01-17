package pencilSketch;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class PencilSketchGUI {
    static { System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    private static Mat currentSketch;
    private static String currentImagePath;
    private static JLabel imageLabel;
    private static Timer animationTimer;
    private static int currentRow;
    private static int pencilX;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("✏️ Pencil Sketch Portrait");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(900, 700);

            imageLabel = new JLabel("Upload an image to sketch", SwingConstants.CENTER);
            imageLabel.setFont(new Font("Serif", Font.BOLD, 20));
            frame.add(imageLabel, BorderLayout.CENTER);

            // Buttons
            JButton uploadButton = new JButton("Choose Image");
            JButton saveButton = new JButton("Save Sketch");
            saveButton.setEnabled(false);

            // Sliders
            JSlider blurSlider = new JSlider(1, 51, 21);
            blurSlider.setMajorTickSpacing(10);
            blurSlider.setPaintTicks(true);
            blurSlider.setPaintLabels(true);
            blurSlider.setBorder(BorderFactory.createTitledBorder("Blur Strength"));

            JSlider contrastSlider = new JSlider(1, 5, 1);
            contrastSlider.setMajorTickSpacing(1);
            contrastSlider.setPaintTicks(true);
            contrastSlider.setPaintLabels(true);
            contrastSlider.setBorder(BorderFactory.createTitledBorder("Contrast"));

            JPanel controlPanel = new JPanel();
            controlPanel.add(uploadButton);
            controlPanel.add(saveButton);
            controlPanel.add(blurSlider);
            controlPanel.add(contrastSlider);

            frame.add(controlPanel, BorderLayout.SOUTH);

            // Upload action
            uploadButton.addActionListener(e -> {
                JFileChooser chooser = new JFileChooser();
                if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    currentImagePath = chooser.getSelectedFile().getAbsolutePath();
                    currentSketch = createPencilSketch(currentImagePath, blurSlider.getValue(), contrastSlider.getValue());
                    animateSketch(currentSketch);
                    saveButton.setEnabled(true);
                }
            });

            // Save action
            saveButton.addActionListener(e -> {
                if (currentSketch != null) {
                    JFileChooser chooser = new JFileChooser();
                    chooser.setDialogTitle("Save Sketch As");
                    chooser.setSelectedFile(new File("sketch_output.jpg"));
                    if (chooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                        String savePath = chooser.getSelectedFile().getAbsolutePath();
                        Imgcodecs.imwrite(savePath, currentSketch);
                        JOptionPane.showMessageDialog(frame,
                                "✅ Sketch saved to: " + savePath,
                                "Save Successful",
                                JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            });

            // Slider actions (live update)
            blurSlider.addChangeListener(e -> {
                if (currentImagePath != null) {
                    currentSketch = createPencilSketch(currentImagePath, blurSlider.getValue(), contrastSlider.getValue());
                    animateSketch(currentSketch);
                }
            });

            contrastSlider.addChangeListener(e -> {
                if (currentImagePath != null) {
                    currentSketch = createPencilSketch(currentImagePath, blurSlider.getValue(), contrastSlider.getValue());
                    animateSketch(currentSketch);
                }
            });

            frame.setVisible(true);
        });
    }

    // Create pencil sketch with adjustable blur & contrast
    private static Mat createPencilSketch(String imagePath, int blurStrength, int contrast) {
        Mat image = Imgcodecs.imread(imagePath);
        Mat gray = new Mat();
        Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);

        Mat inverted = new Mat();
        Core.bitwise_not(gray, inverted);

        Mat blurred = new Mat();
        Imgproc.GaussianBlur(inverted, blurred, new Size(blurStrength, blurStrength), 0);

        Mat sketch = new Mat();
        Core.divide(gray, new MatOfDouble(255.0 * contrast).mul(blurred), sketch, 256);

        return sketch;
    }

    // Animate sketch line by line with pencil movement
    private static void animateSketch(Mat sketch) {
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }
        BufferedImage sketchImage = matToBufferedImage(sketch);

        // Start with a blank white canvas
        BufferedImage canvas = new BufferedImage(sketchImage.getWidth(), sketchImage.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D gCanvas = canvas.createGraphics();
        gCanvas.setColor(Color.WHITE);
        gCanvas.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        currentRow = 0;

        animationTimer = new Timer(15, e -> {
            if (currentRow < sketchImage.getHeight()) {
                // Copy one row of pixels from sketch to canvas
                canvas.getGraphics().drawImage(sketchImage,
                        0, currentRow, sketchImage.getWidth(), currentRow + 1,
                        0, currentRow, sketchImage.getWidth(), currentRow + 1,
                        null);

                imageLabel.setIcon(new ImageIcon(canvas));
                currentRow++;
            } else {
                animationTimer.stop();
                imageLabel.setIcon(new ImageIcon(sketchImage)); // final full sketch
            }
        });
        animationTimer.start();
    }
    // Convert Mat to BufferedImage
    private static BufferedImage matToBufferedImage(Mat mat) {
        int type = BufferedImage.TYPE_BYTE_GRAY;
        if (mat.channels() > 1) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }
        int bufferSize = mat.channels() * mat.cols() * mat.rows();
        byte[] b = new byte[bufferSize];
        mat.get(0, 0, b);
        BufferedImage image = new BufferedImage(mat.cols(), mat.rows(), type);
        image.getRaster().setDataElements(0, 0, mat.cols(), mat.rows(), b);
        return image;
    }
}