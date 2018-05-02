/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package frontend;

import backend.YetAnotherFileSharingAppClient;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author baronesc
 */
public class ShareFileFrame extends javax.swing.JFrame {
    
    private final JFrame parent;
    private final String fileName;
    private final YetAnotherFileSharingAppClient clientInstance;
    private final DefaultTableModel usersTblModel;

    /**
     * Creates new form ShareFileFrame
     * @param parent
     * @param fileName
     * @param clientInstance
     */
    public ShareFileFrame(JFrame parent, String fileName, YetAnotherFileSharingAppClient clientInstance) {
        
        initComponents();
        
        this.parent = parent;
        this.fileName = fileName;
        this.clientInstance = clientInstance;
        this.usersTblModel = (DefaultTableModel) usersTbl.getModel();
        
        updateTableOfUsers();
    }
    
    private void updateTableOfUsers() {
        
        String[] users = clientInstance.getListOfUsers();
        
        usersTblModel.setRowCount(0);
        for (String userName : users) {
            usersTblModel.addRow(new Object[]
            {
                userName
            });
        }
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        usersTbl = new javax.swing.JTable() {
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        selectUserBtn = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
        });

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("All Users"));

        usersTbl.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {},
            new String [] {
                "Username"
            }
        ));
        jScrollPane1.setViewportView(usersTbl);

        selectUserBtn.setText("Select User");
        selectUserBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectUserBtnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 476, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(selectUserBtn)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 426, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(selectUserBtn))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
        parent.setEnabled(true);
        parent.toFront();
    }//GEN-LAST:event_formWindowClosed

    private void selectUserBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectUserBtnActionPerformed
        
        if (usersTbl.getSelectedRow() < 0) {
            JOptionPane.showMessageDialog(new JFrame(), "No user selected! Please select a user.", "Error!",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String username = (String) usersTblModel.getValueAt(usersTbl.getSelectedRow(), 0);
        if (!clientInstance.ShareFileWith(fileName, username)) {
            JOptionPane.showMessageDialog(new JFrame(), "File not found!", "Error!", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_selectUserBtnActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton selectUserBtn;
    private javax.swing.JTable usersTbl;
    // End of variables declaration//GEN-END:variables
}