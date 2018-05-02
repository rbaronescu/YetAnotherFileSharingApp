/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package frontend;

import backend.FileInfo;
import backend.YetAnotherFileSharingAppClient;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author baronesc
 */
public class NotificationsFrame extends javax.swing.JFrame {
    
    private final JFrame parent;
    private final YetAnotherFileSharingAppClient clientInstance;
    private final DefaultTableModel notificationsTblModel;

    /**
     * Creates new form NotificationsFrame
     * @param parent
     * @param clientInstance
     */
    public NotificationsFrame(JFrame parent, YetAnotherFileSharingAppClient clientInstance) {
        
        initComponents();
        
        this.parent = parent;
        this.clientInstance = clientInstance;
        this.notificationsTblModel = (DefaultTableModel) notificationsTbl.getModel();
        
        updateTableOfNotifications();
    }
    
    private void updateTableOfNotifications() {

        FileInfo[] invitations = clientInstance.getUserInvitations();

        notificationsTblModel.setRowCount(0);
        for (FileInfo fileInfo : invitations) {
            notificationsTblModel.addRow(new Object[]
            {
                fileInfo.getOwner() + " invites you to collaborate to " + fileInfo.getFileName()
            });
        }
        
        int noOfNotif = notificationsTbl.getRowCount();
        if (noOfNotif > 1) {
            numberOfNotificationsLbl.setText("You have " + noOfNotif + " notifications!");
        } else if (noOfNotif == 1) {
            numberOfNotificationsLbl.setText("You have " + noOfNotif + " notification!");
        } else {
            numberOfNotificationsLbl.setText("There are no notifications.");
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
        notificationsTbl = new javax.swing.JTable();
        acceptBtn = new javax.swing.JButton();
        declineBtn = new javax.swing.JButton();
        numberOfNotificationsLbl = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("YetAnotherFileSharingApp");
        addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                formPropertyChange(evt);
            }
        });
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
        });

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Your Notifications"));

        notificationsTbl.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {},
            new String [] {
                "Notification"
            }
        ));
        jScrollPane1.setViewportView(notificationsTbl);

        acceptBtn.setText("Accept");
        acceptBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                acceptBtnActionPerformed(evt);
            }
        });

        declineBtn.setText("Decline");
        declineBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                declineBtnActionPerformed(evt);
            }
        });

        numberOfNotificationsLbl.setText("There are no notifications.");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 383, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addComponent(numberOfNotificationsLbl)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(declineBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(acceptBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(acceptBtn)
                    .addComponent(declineBtn)
                    .addComponent(numberOfNotificationsLbl))
                .addContainerGap())
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

    private void respondToNotification(String response) {
        
        String invitation = (String) notificationsTblModel.getValueAt(notificationsTbl.getSelectedRow(), 0);
        String[] tokens = invitation.split(" ");
        
        String fileName = "";
        for (int i = 6; i < tokens.length; i++) {
            fileName += tokens[i] + " ";
        }      
        fileName = fileName.trim();
  
        clientInstance.respondToNotification(fileName, tokens[0], response);
        updateTableOfNotifications();
        
        if (notificationsTbl.getRowCount() < 1) {
            this.dispose();
        }
    }
    
    private void acceptBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_acceptBtnActionPerformed
        
        if (notificationsTbl.getSelectedRow() < 0) {
            JOptionPane.showMessageDialog(new JFrame(), "No notification selected! Please select one.", "Error!",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        respondToNotification("accept");
    }//GEN-LAST:event_acceptBtnActionPerformed

    private void declineBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_declineBtnActionPerformed
        if (notificationsTbl.getSelectedRow() < 0) {
            JOptionPane.showMessageDialog(new JFrame(), "No notification selected! Please select one.", "Error!",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        respondToNotification("decline");
    }//GEN-LAST:event_declineBtnActionPerformed

    private void formPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_formPropertyChange

    }//GEN-LAST:event_formPropertyChange

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton acceptBtn;
    private javax.swing.JButton declineBtn;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable notificationsTbl;
    private javax.swing.JLabel numberOfNotificationsLbl;
    // End of variables declaration//GEN-END:variables
}