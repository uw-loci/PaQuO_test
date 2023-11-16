import javax.swing.*
import java.awt.GridLayout

// Create a JFrame as the main window
JFrame frame = new JFrame("Input Dialog")
frame.setSize(300, 200)
frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE) // Corrected to dispose only the frame

// Panel to hold the components, using GridLayout for better layout
JPanel panel = new JPanel(new GridLayout(0, 2))
frame.add(panel)

// Function to add labels and text fields to the panel
def addRow(String label, JPanel panel) {
    JLabel lbl = new JLabel(label)
    JTextField txt = new JTextField(20)
    panel.add(lbl)
    panel.add(txt)
    return txt
}

// Adding rows for inputs
def sampleNameField = addRow("Sample Name:", panel)
def metadataField = addRow("Metadata (PDAC, pancreatitis):", panel)
def projectNameField = addRow("Overall Project Name:", panel)

// Button to submit the information
JButton submitButton = new JButton("Submit")
panel.add(submitButton)

// Data structure to store the inputs
def inputData = [:]

// Action for the submit button
submitButton.addActionListener({ 
    inputData.sampleName = sampleNameField.text
    inputData.metadata = metadataField.text
    inputData.projectName = projectNameField.text

    // Print or process inputData as needed
    println inputData

    // Close the frame after submission
    frame.dispose()
})

// Display the window
frame.setVisible(true)
