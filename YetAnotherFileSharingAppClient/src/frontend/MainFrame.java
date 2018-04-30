/*
 * YetAnotherFileSharingAppClient - The client side.
 */
package frontend;

import backend.YetAnotherFileSharingAppClient;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 *
 * @author Robert Baronescu
 */
public class MainFrame extends javax.swing.JFrame {
    
    YetAnotherFileSharingAppClient clientInstance;

    /**
     * Creates new form MainFrame
     * @param clientInstance
     */
    public MainFrame(YetAnotherFileSharingAppClient clientInstance) {
        
        this.clientInstance = clientInstance;
        
        initComponents();
        updateListOfRemoteFiles();
    }
    
    private void updateListOfRemoteFiles() {

        remoteFilesLst.setListData(clientInstance.getRemoteFilesInfo());
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        listFilesPopUpMnu = new javax.swing.JPopupMenu();
        openFileMnuItm = new javax.swing.JMenuItem();
        deleteFileMnuItm = new javax.swing.JMenuItem();
        fileChooser = new javax.swing.JFileChooser();
        mainPnl = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        remoteFilesLst = new javax.swing.JList<>();
        jMenuBar1 = new javax.swing.JMenuBar();
        fileMnu = new javax.swing.JMenu();
        newFileMnuItm = new javax.swing.JMenuItem();
        uploadFileMnuItm = new javax.swing.JMenuItem();
        exitMnuItm = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();

        openFileMnuItm.setText("Open File");
        openFileMnuItm.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openFileMnuItmActionPerformed(evt);
            }
        });
        listFilesPopUpMnu.add(openFileMnuItm);

        deleteFileMnuItm.setText("Remove File");
        deleteFileMnuItm.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteFileMnuItmActionPerformed(evt);
            }
        });
        listFilesPopUpMnu.add(deleteFileMnuItm);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("YetAnotherFileSharingApp");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowActivated(java.awt.event.WindowEvent evt) {
                formWindowActivated(evt);
            }
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        mainPnl.setBorder(javax.swing.BorderFactory.createTitledBorder("All your files"));

        remoteFilesLst.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                remoteFilesLstMouseClicked(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                remoteFilesLstMouseReleased(evt);
            }
        });
        jScrollPane1.setViewportView(remoteFilesLst);

        javax.swing.GroupLayout mainPnlLayout = new javax.swing.GroupLayout(mainPnl);
        mainPnl.setLayout(mainPnlLayout);
        mainPnlLayout.setHorizontalGroup(
            mainPnlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPnlLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 575, Short.MAX_VALUE)
                .addContainerGap())
        );
        mainPnlLayout.setVerticalGroup(
            mainPnlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPnlLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 556, Short.MAX_VALUE)
                .addContainerGap())
        );

        fileMnu.setText("File");

        newFileMnuItm.setText("New Empty File");
        newFileMnuItm.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newFileMnuItmActionPerformed(evt);
            }
        });
        fileMnu.add(newFileMnuItm);

        uploadFileMnuItm.setText("Upload Existing File");
        uploadFileMnuItm.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                uploadFileMnuItmActionPerformed(evt);
            }
        });
        fileMnu.add(uploadFileMnuItm);

        exitMnuItm.setText("Exit");
        exitMnuItm.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitMnuItmActionPerformed(evt);
            }
        });
        fileMnu.add(exitMnuItm);

        jMenuBar1.add(fileMnu);

        jMenu2.setText("Edit");
        jMenuBar1.add(jMenu2);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(mainPnl, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(mainPnl, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void remoteFilesLstMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_remoteFilesLstMouseClicked

        if (evt.isPopupTrigger())
            return;
        
        if (evt.getClickCount() < 2)
            return;
        
        if (remoteFilesLst.getSelectedIndex() == -1)
            return;
        
        String fileName = remoteFilesLst.getSelectedValue();
        if (!clientInstance.editRemoteFile(fileName)) {
            JOptionPane.showMessageDialog(new JFrame(), "Remote file could not be opened!", "Error!",
                    JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_remoteFilesLstMouseClicked

    private void remoteFilesLstMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_remoteFilesLstMouseReleased
        
        remoteFilesLst.setSelectedIndex(remoteFilesLst.locationToIndex(evt.getPoint()));
        
        if (evt.isPopupTrigger()) {
            listFilesPopUpMnu.show(this, evt.getX() + 36, evt.getY() + 90);
        }
    }//GEN-LAST:event_remoteFilesLstMouseReleased

    private void openFileMnuItmActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openFileMnuItmActionPerformed
        
        String fileName = remoteFilesLst.getSelectedValue();
        
        if (!clientInstance.editRemoteFile(fileName)) {
            JOptionPane.showMessageDialog(new JFrame(), "Remote file could not be openend!", "Error!",
                    JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_openFileMnuItmActionPerformed

    private void exitMnuItmActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitMnuItmActionPerformed
        clientInstance.closeConnection();
        System.exit(0);
    }//GEN-LAST:event_exitMnuItmActionPerformed

    private void newFileMnuItmActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newFileMnuItmActionPerformed
        (new NewEmptyFileFrame(this, clientInstance)).setVisible(true);
        this.setEnabled(false);
    }//GEN-LAST:event_newFileMnuItmActionPerformed

    private void deleteFileMnuItmActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteFileMnuItmActionPerformed
        
        String fileName = remoteFilesLst.getSelectedValue();
        
        if (!clientInstance.removeFile(fileName)) {
            JOptionPane.showMessageDialog(new JFrame(), "Remote file does not exist!", "Error!",
                    JOptionPane.ERROR_MESSAGE);
        }
        
        updateListOfRemoteFiles();
    }//GEN-LAST:event_deleteFileMnuItmActionPerformed

    private void formWindowActivated(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowActivated
        updateListOfRemoteFiles();
    }//GEN-LAST:event_formWindowActivated

    private void uploadFileMnuItmActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_uploadFileMnuItmActionPerformed
        
        int retVal = fileChooser.showOpenDialog(this);
        
        if (retVal == JFileChooser.APPROVE_OPTION) {
            
            File f = fileChooser.getSelectedFile();
            if (clientInstance.uploadFile(f)) {
                JOptionPane.showMessageDialog(new JFrame(), "File uploaded succesfully.", "Info",
                    JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(new JFrame(), "Error uploading file!", "Error!",
                    JOptionPane.ERROR_MESSAGE);
            }
            
            updateListOfRemoteFiles();
        }
        
    }//GEN-LAST:event_uploadFileMnuItmActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        clientInstance.closeConnection();
    }//GEN-LAST:event_formWindowClosing

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem deleteFileMnuItm;
    private javax.swing.JMenuItem exitMnuItm;
    private javax.swing.JFileChooser fileChooser;
    private javax.swing.JMenu fileMnu;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JPopupMenu listFilesPopUpMnu;
    private javax.swing.JPanel mainPnl;
    private javax.swing.JMenuItem newFileMnuItm;
    private javax.swing.JMenuItem openFileMnuItm;
    private javax.swing.JList<String> remoteFilesLst;
    private javax.swing.JMenuItem uploadFileMnuItm;
    // End of variables declaration//GEN-END:variables
}
