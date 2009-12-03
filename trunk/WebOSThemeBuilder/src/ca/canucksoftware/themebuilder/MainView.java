/*
 * WebOSThemeBuilderView.java
 */

package ca.canucksoftware.themebuilder;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.*;
import java.awt.image.*;
import javax.swing.*;
import org.jdesktop.application.Action;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JFileChooser;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.DefaultListModel;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Dimension;
import java.net.URL;
import java.util.TimerTask;
import java.util.Timer;
import java.awt.Component;
import java.awt.Container;
import javax.swing.UIManager;
import javax.swing.JButton;
import java.io.File;
import java.util.LinkedList;
import javax.swing.table.DefaultTableModel;
import javax.swing.ImageIcon;
import com.twicom.qdparser.*;
import java.util.zip.*;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import javax.swing.JOptionPane;

/**
 * The application's main frame.
 */
public class MainView extends FrameView {
    private boolean pageLoaded;
    private File zipOutDir;
    private Image bg;
    private Timer t;
    private LinkedList<File> ss;
    private LinkedList<IconEntry> icons;
    private LinkedList<FileEntry> files;
    private LinkedList<File> patches;
    private PatchData pData;
    private File wallpaper;
    private File prevDir;
    private LinkedList<String> outNames;

    public MainView(SingleFrameApplication app) {
        super(app);
        URL imgURL = getClass().getResource("resources/icon.png");
        getFrame().setIconImage(new ImageIcon(imgURL).getImage());
        URL bgURL = getClass().getResource("resources/bg.jpg");
        bg = new ImageIcon(bgURL).getImage();
        pageLoaded = false;
        initComponents();
        zipOutDir = null;
        ss = new LinkedList<File>();
        icons = new LinkedList<IconEntry>();
        files = new LinkedList<FileEntry>();
        patches = new LinkedList<File>();
        outNames = new LinkedList<String>();
        pData = new PatchData();
        prevDir = null;
        wallpaper = null;
        t = new Timer();
        jButton6.setContentAreaFilled(false);
        jButton6.setBorderPainted(false);
        jButton6.setEnabled(false);
        jButton6.setIcon(null);
        WindowListener wl = new WindowListener() {
            public void windowOpened(WindowEvent e) {}
            public void windowClosing(WindowEvent e) {
                if(zipOutDir!=null && zipOutDir.exists()) {
                    deleteDirectory(zipOutDir);
                    if(zipOutDir.exists()) {
                        zipOutDir.delete();
                    }
                }
                System.exit(0);
            }
            public void windowClosed(WindowEvent e) {
                System.exit(0);
            }
            public void windowIconified(WindowEvent e) {}
            public void windowDeiconified(WindowEvent e) {}
            public void windowActivated(WindowEvent e) {}
            public void windowDeactivated(WindowEvent e) {}
        };
        super.getFrame().addWindowListener(wl);
        t.schedule(new DoDelayedLoad(), 50);
    }

    @Action
    public void showAboutBox() {
        if (aboutBox == null) {
            JFrame mainFrame = WebOSThemeBuilderApp.getApplication().getMainFrame();
            aboutBox = new AboutBox(mainFrame);
            aboutBox.setLocationRelativeTo(mainFrame);
        }
        WebOSThemeBuilderApp.getApplication().show(aboutBox);
    }

    private File loadFileChooser(javax.swing.filechooser.FileFilter ff, boolean isSave) {
        File result;
        JFileChooser fc = new JFileChooser(); //Create a file chooser
        fc.setAcceptAllFileFilterUsed(false);
        fc.setMultiSelectionEnabled(false);
        if(prevDir!=null)
            fc.setCurrentDirectory(prevDir);
        disableNewFolderButton(fc);
        fc.setFileFilter(ff);
        if(!isSave) {
            fc.setDialogTitle("");
            if (fc.showDialog(null, "Select") == JFileChooser.APPROVE_OPTION) {
                result = fc.getSelectedFile();
            } else {
                result = null;
            }
        } else {
            fc.setDialogTitle("Save As...");
            if(fc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                result = fc.getSelectedFile();
            } else {
                result = null;
            }
        }
        if(result!=null)
            prevDir = result.getParentFile();
        return result;
    }

    private void disableNewFolderButton(Container c) {
        int len = c.getComponentCount();
        for(int i=0; i<len; i++) {
            Component comp = c.getComponent(i);
            if(comp instanceof JButton) {
                JButton b = (JButton)comp;
                Icon icon = b.getIcon();
                if(icon != null && (icon == UIManager.getIcon("FileChooser.newFolderIcon")
                        || icon == UIManager.getIcon("FileChooser.upFolderIcon")))
                    b.setEnabled(false);
            } else if (comp instanceof Container) {
                disableNewFolderButton((Container)comp);
            }
        }
    }

    private void updateSSList() {
        LinkedList<String> names = new LinkedList<String>();
        for(int i=0; i<ss.size(); i++) {
            names.add(getFilename(ss.get(i)));
        }
        jList2.setListData(names.toArray());
    }

    private String getFilename(File f) {
        return getFilename(f.getName());
    }
    private String getFilename(String f) {
        String result = f;
        if(result.lastIndexOf("/")!=-1) {
            result = result.substring(result.lastIndexOf("/")+1);
        }
        if(result.lastIndexOf("\\")!=-1) {
            result = result.substring(result.lastIndexOf("\\")+1);
        }
        return result;
    }

    public boolean deleteDirectory(File path) {
        if(path.exists()) {
            File[] curr = path.listFiles();
            for(int i=0; i<curr.length; i++) {
                if(curr[i].isDirectory()) {
                    deleteDirectory(curr[i]);
                } else {
                    curr[i].delete();
                }
            }
        }
        return(path.delete());
    }

    private void changeState(boolean val) {
        jTextField1.setEnabled(val);
        jTextField2.setEnabled(val);
        jTextField3.setEnabled(val);
        jTextField4.setEnabled(val);
        jTextArea1.setEnabled(val);
        jList2.setEnabled(val);
        jButton2.setEnabled(val);
        jButton1.setEnabled(val);
        jTextField5.setEnabled(val);
        jButton6.setEnabled(val);
        jButton7.setEnabled(val);
        jButton5.setEnabled(val);
        jButton3.setEnabled(val);
        jTabbedPane1.setEnabled(val);
        if(val) {
            jButton3.setText("Build Theme Zip");
        } else {
            jButton3.setText("Please wait...");
        }
        getRootPane().requestFocus();
    }

    private void addToZip(ZipOutputStream out, File f, String s) throws Exception {
        byte[] buf = new byte[2048];
        FileInputStream in = new FileInputStream(f);
        out.putNextEntry(new ZipEntry(s));
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        out.closeEntry();
        in.close();
    }

    private String uniqueFilename(String name, int i) {
        String s, result;
        if(i==0) {
            s = name;
        } else {
            int index = name.lastIndexOf(".");
            if(index>-1) {
                String ext = name.substring(name.lastIndexOf(".")+1);
                s = name.substring(0, index) + "-" + i + "." + ext;
            } else {
                s = name + "-" + i;
            }
        }
        if(!outNames.contains(s)) {
            outNames.add(s);
            result = s;
        } else {
            result = uniqueFilename(name, i+1);
        }
        return result;
    }

    private boolean infoGood() {
        if(jTextField1.getText().length()==0) {
            JOptionPane.showMessageDialog(mainPanel, "ERROR: Missing theme name.");
            return false;
        }
        if(jTextField2.getText().length()==0) {
            JOptionPane.showMessageDialog(mainPanel, "ERROR: Missing theme version.");
            return false;
        }
        if(jTextField3.getText().length()==0) {
            JOptionPane.showMessageDialog(mainPanel, "ERROR: Missing theme creator.");
            return false;
        }
        if(jTextField4.getText().length()==0) {
            JOptionPane.showMessageDialog(mainPanel, "ERROR: Missing theme homepage url.");
            return false;
        }
        if(jTextArea1.getText().length()==0) {
            JOptionPane.showMessageDialog(mainPanel, "ERROR: Missing theme description.");
            return false;
        }
        if(jList2.getModel().getSize()==0) {
            JOptionPane.showMessageDialog(mainPanel, "ERROR: Missing screenshots.\nAt least 1 screenshot" +
                    " is required.");
            return false;
        }
        return true;
    }

    private void reset() {
        if(zipOutDir!=null && zipOutDir.exists()) {
            deleteDirectory(zipOutDir);
            if(zipOutDir.exists()) {
                zipOutDir.delete();
            }
        }
        jTextField1.setText("");
        jTextField2.setText("");
        jTextField3.setText("");
        jTextField4.setText("");
        jTextField5.setText("");
        jTextArea1.setText("");
        jList2.setListData(new Object[] {});
        ss.clear();
        files.clear();
        pData = new PatchData();
        icons.clear();
        patches.clear();
        wallpaper = null;
        DefaultTableModel dtm = (DefaultTableModel) jTable1.getModel();
        while(dtm.getRowCount()>0)
            dtm.removeRow(0);
        dtm = (DefaultTableModel) jTable2.getModel();
        while(dtm.getRowCount()>0)
            dtm.removeRow(0);
        dtm = (DefaultTableModel) jTable3.getModel();
        while(dtm.getRowCount()>0)
            dtm.removeRow(0);
    }

    private void openZip(File zip) {
        reset();
        try {
            int i;
            File curr;
            String s = getFilename(zip);
            File dir = new File(s);
            zipOutDir = dir;
            extractTheme(zip, dir);
            File xml = new File(dir, "theme.xml");
            if(xml.exists()) {
                XMLHandler theme = new XMLHandler(xml);
                TaggedElement info = theme.getRoot().find("themeinfo");
                jTextField1.setText(theme.getContent(info.find("name")));
                jTextField2.setText(theme.getContent(info.find("version")));
                jTextField3.setText(theme.getContent(info.find("creator")));
                jTextField4.setText(theme.getContent(info.find("website")));
                jTextArea1.setText(theme.getContent(info.find("description")));
                TaggedElement screenshots = info.find("screenshots");
                for(i=0; i<screenshots.elements(); i++) {
                    curr = new File(dir, theme.getContent(screenshots.getChild(i)));
                    if(curr.exists()) {
                        ss.add(curr);
                    }
                }
                updateSSList();
                TaggedElement content = theme.getRoot().find("themelist");
                DefaultTableModel fileDTM = (DefaultTableModel) jTable1.getModel();
                DefaultTableModel iconDTM = (DefaultTableModel) jTable2.getModel();
                DefaultTableModel patchDTM = (DefaultTableModel) jTable3.getModel();
                for(i=0; i<content.elements(); i++) {
                    TaggedElement currEle = (TaggedElement) content.getChild(i);
                    if(currEle.getTag().equalsIgnoreCase("file")) {
                        Element src = currEle.find("filename");
                        Element dest = currEle.find("destination");
                        if(src!=null && dest !=null) {
                            curr = new File(dir, theme.getContent(src));
                            if(curr.exists()) {
                                FileEntry fe = new FileEntry();
                                fe.file = curr;
                                fe.dest = theme.getContent(dest);
                                files.add(fe);
                                fileDTM.addRow(new Object[] {fe.file.getPath(), fe.dest});
                            }
                        }
                    } else if(currEle.getTag().equalsIgnoreCase("wallpaper")) {
                        Element wp = currEle.find("image");
                        if(wp!=null) {
                            curr = new File(dir, theme.getContent(wp));
                            if(curr.exists()) {
                                jTextField5.setText(curr.getPath());
                                wallpaper = curr;
                            }
                        }
                    } else if(currEle.getTag().equalsIgnoreCase("iconset")) {
                        for(int j=0; j<currEle.elements(); j++) {
                            TaggedElement currIcon = (TaggedElement) currEle.getChild(j);
                            Element src = currIcon.find("image");
                            Element id = currIcon.find("appid");
                            if(src!=null && id!=null) {
                                curr = new File(dir, theme.getContent(src));
                                if(curr.exists()) {
                                    IconEntry ie = new IconEntry();
                                    ie.image = curr;
                                    ie.appID = theme.getContent(id);
                                    icons.add(ie);
                                    iconDTM.addRow(new Object[] {ie.image.getPath(), ie.appID});
                                }
                            }
                        }
                    } else if(currEle.getTag().equalsIgnoreCase("patch")) {
                        Element diff = currEle.find("diff");
                        if(diff!=null) {
                            curr = new File(dir, theme.getContent(diff));
                            if(curr.exists()) {
                                boolean add = true;
                                String temp = getFilename(curr);
                                if(temp.equalsIgnoreCase(getFilename(pData.CARRIER_PATCH))) {
                                    if(pData.parsePatch(curr, pData.CARRIER_PATCH)) {
                                        add = false;
                                    }
                                } else if(temp.equalsIgnoreCase(getFilename(pData.TEXTCOLOUR_PATCH))) {
                                    if(pData.parsePatch(curr, pData.TEXTCOLOUR_PATCH)) {
                                        add = false;
                                    }
                                } else if(temp.equalsIgnoreCase(getFilename(pData.OPACITY_PATCH))) {
                                    if(pData.parsePatch(curr, pData.OPACITY_PATCH)) {
                                        add = false;
                                    }
                                }
                                if(add) {
                                    patches.add(curr);
                                    patchDTM.addRow(new Object[] {curr.getPath()});
                                } else {
                                    curr.delete();
                                }
                                LinkedList<String> p = pData.list();
                                for(int l=0; l<p.size(); l++) {
                                    patchDTM.insertRow(l, new Object[] {p.get(l)});
                                }
                            }
                        }
                    }
                }
                xml.delete();
            }
        } catch(Exception e) {
            JOptionPane.showMessageDialog(mainPanel, e.getMessage());
        }
    }

    private void extractTheme(File open, File outDir) throws Exception {
        final int BUFFER = 2048;
        int count;
        byte data[] = new byte[BUFFER];
        outDir.mkdirs();
        ZipFile zip = new ZipFile(open);
        Enumeration entries = zip.entries();
        while(entries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry)entries.nextElement();
            if(entry.isDirectory()) {
                File dir = new File(outDir, entry.getName());
                dir.mkdirs();
            } else {
                InputStream is = zip.getInputStream(entry);
                File out = new File(outDir, entry.getName());
                if(!out.getParentFile().isDirectory()) {
                    out.getParentFile().mkdirs();
                }
                FileOutputStream fos = new FileOutputStream(out);
                while ((count = is.read(data,0,BUFFER)) != -1) {
                     fos.write(data,0,count);
                }
                fos.flush();
                fos.close();
                is.close();
            }
        }
        zip.close();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new ImagePanel(bg);
        jLayeredPane1 = new javax.swing.JLayeredPane();
        jLayeredPane2 = new TranslucentPanel();
        jLabel5 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jTextField4 = new javax.swing.JTextField();
        jButton2 = new javax.swing.JButton();
        jLabel7 = new javax.swing.JLabel();
        jTextField3 = new javax.swing.JTextField();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jList2 = new javax.swing.JList();
        jLabel9 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        jTextField2 = new javax.swing.JTextField();
        jButton3 = new javax.swing.JButton();
        jLayeredPane3 = new TranslucentPanel();
        jTextField5 = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        jButton7 = new javax.swing.JButton();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jLayeredPane4 = new javax.swing.JLayeredPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jLayeredPane5 = new javax.swing.JLayeredPane();
        jScrollPane4 = new javax.swing.JScrollPane();
        jTable2 = new javax.swing.JTable();
        jLayeredPane6 = new javax.swing.JLayeredPane();
        jScrollPane5 = new javax.swing.JScrollPane();
        jTable3 = new javax.swing.JTable();
        jButton5 = new javax.swing.JButton();
        jButton6 = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JSeparator();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        jMenu1 = new javax.swing.JMenu();
        jMenuItem4 = new javax.swing.JMenuItem();
        jMenuItem3 = new javax.swing.JMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();

        mainPanel.setName("mainPanel"); // NOI18N

        jLayeredPane1.setName("jLayeredPane1"); // NOI18N

        jLayeredPane2.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jLayeredPane2.setName("jLayeredPane2"); // NOI18N

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(ca.canucksoftware.themebuilder.WebOSThemeBuilderApp.class).getContext().getResourceMap(MainView.class);
        jLabel5.setFont(resourceMap.getFont("jLabel5.font")); // NOI18N
        jLabel5.setText(resourceMap.getString("jLabel5.text")); // NOI18N
        jLabel5.setName("jLabel5"); // NOI18N
        jLabel5.setBounds(370, 20, 100, 30);
        jLayeredPane2.add(jLabel5, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel3.setFont(resourceMap.getFont("jLabel3.font")); // NOI18N
        jLabel3.setText(resourceMap.getString("jLabel3.text")); // NOI18N
        jLabel3.setName("jLabel3"); // NOI18N
        jLabel3.setBounds(10, 90, 80, 20);
        jLayeredPane2.add(jLabel3, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jTextField4.setText(resourceMap.getString("jTextField4.text")); // NOI18N
        jTextField4.setName("jTextField4"); // NOI18N
        jTextField4.setBounds(110, 120, 250, 22);
        jLayeredPane2.add(jTextField4, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jButton2.setBackground(resourceMap.getColor("jButton2.background")); // NOI18N
        jButton2.setIcon(resourceMap.getIcon("jButton2.icon")); // NOI18N
        jButton2.setName("jButton2"); // NOI18N
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });
        jButton2.setBounds(850, 50, 30, 30);
        jLayeredPane2.add(jButton2, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel7.setFont(resourceMap.getFont("jLabel7.font")); // NOI18N
        jLabel7.setText(resourceMap.getString("jLabel7.text")); // NOI18N
        jLabel7.setName("jLabel7"); // NOI18N
        jLabel7.setBounds(640, 20, 190, 30);
        jLayeredPane2.add(jLabel7, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jTextField3.setText(resourceMap.getString("jTextField3.text")); // NOI18N
        jTextField3.setName("jTextField3"); // NOI18N
        jTextField3.setBounds(110, 90, 200, 22);
        jLayeredPane2.add(jTextField3, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jScrollPane3.setName("jScrollPane3"); // NOI18N

        jTextArea1.setColumns(20);
        jTextArea1.setFont(resourceMap.getFont("jTextArea1.font")); // NOI18N
        jTextArea1.setLineWrap(true);
        jTextArea1.setRows(5);
        jTextArea1.setWrapStyleWord(true);
        jTextArea1.setName("jTextArea1"); // NOI18N
        jScrollPane3.setViewportView(jTextArea1);

        jScrollPane3.setBounds(370, 50, 250, 90);
        jLayeredPane2.add(jScrollPane3, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel1.setFont(resourceMap.getFont("jLabel1.font")); // NOI18N
        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N
        jLabel1.setBounds(10, 30, 90, 20);
        jLayeredPane2.add(jLabel1, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jScrollPane2.setName("jScrollPane2"); // NOI18N

        jList2.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jList2.setName("jList2"); // NOI18N
        jScrollPane2.setViewportView(jList2);

        jScrollPane2.setBounds(640, 50, 200, 90);
        jLayeredPane2.add(jScrollPane2, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel9.setFont(resourceMap.getFont("jLabel9.font")); // NOI18N
        jLabel9.setText(resourceMap.getString("jLabel9.text")); // NOI18N
        jLabel9.setName("jLabel9"); // NOI18N
        jLabel9.setBounds(10, 0, 100, 20);
        jLayeredPane2.add(jLabel9, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel4.setFont(resourceMap.getFont("jLabel4.font")); // NOI18N
        jLabel4.setText(resourceMap.getString("jLabel4.text")); // NOI18N
        jLabel4.setName("jLabel4"); // NOI18N
        jLabel4.setBounds(10, 120, 80, 20);
        jLayeredPane2.add(jLabel4, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jTextField1.setText(resourceMap.getString("jTextField1.text")); // NOI18N
        jTextField1.setName("jTextField1"); // NOI18N
        jTextField1.setBounds(110, 30, 200, 22);
        jLayeredPane2.add(jTextField1, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jButton1.setBackground(resourceMap.getColor("jButton1.background")); // NOI18N
        jButton1.setIcon(resourceMap.getIcon("jButton1.icon")); // NOI18N
        jButton1.setText(resourceMap.getString("jButton1.text")); // NOI18N
        jButton1.setName("jButton1"); // NOI18N
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jButton1.setBounds(850, 90, 30, 30);
        jLayeredPane2.add(jButton1, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel2.setFont(resourceMap.getFont("jLabel2.font")); // NOI18N
        jLabel2.setText(resourceMap.getString("jLabel2.text")); // NOI18N
        jLabel2.setName("jLabel2"); // NOI18N
        jLabel2.setBounds(10, 60, 70, 20);
        jLayeredPane2.add(jLabel2, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jTextField2.setText(resourceMap.getString("jTextField2.text")); // NOI18N
        jTextField2.setName("jTextField2"); // NOI18N
        jTextField2.setBounds(110, 60, 80, 22);
        jLayeredPane2.add(jTextField2, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLayeredPane2.setBounds(10, 10, 900, 160);
        jLayeredPane1.add(jLayeredPane2, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jButton3.setBackground(resourceMap.getColor("jButton3.background")); // NOI18N
        jButton3.setFont(resourceMap.getFont("jButton3.font")); // NOI18N
        jButton3.setText(resourceMap.getString("jButton3.text")); // NOI18N
        jButton3.setName("jButton3"); // NOI18N
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });
        jButton3.setBounds(380, 620, 170, 30);
        jLayeredPane1.add(jButton3, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLayeredPane3.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jLayeredPane3.setName("jLayeredPane3"); // NOI18N

        jTextField5.setName("jTextField5"); // NOI18N
        jTextField5.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                jTextField5MousePressed(evt);
            }
        });
        jTextField5.setBounds(300, 20, 500, 22);
        jLayeredPane3.add(jTextField5, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel8.setFont(resourceMap.getFont("jLabel8.font")); // NOI18N
        jLabel8.setText(resourceMap.getString("jLabel8.text")); // NOI18N
        jLabel8.setName("jLabel8"); // NOI18N
        jLabel8.setBounds(10, 0, 130, 20);
        jLayeredPane3.add(jLabel8, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jButton7.setBackground(resourceMap.getColor("jButton7.background")); // NOI18N
        jButton7.setIcon(resourceMap.getIcon("jButton7.icon")); // NOI18N
        jButton7.setName("jButton7"); // NOI18N
        jButton7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton7ActionPerformed(evt);
            }
        });
        jButton7.setBounds(840, 140, 40, 40);
        jLayeredPane3.add(jButton7, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jTabbedPane1.setBackground(resourceMap.getColor("jTabbedPane1.background")); // NOI18N
        jTabbedPane1.setName("jTabbedPane1"); // NOI18N
        jTabbedPane1.setOpaque(true);
        jTabbedPane1.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jTabbedPane1StateChanged(evt);
            }
        });

        jLayeredPane4.setBackground(resourceMap.getColor("jLayeredPane4.background")); // NOI18N
        jLayeredPane4.setName("jLayeredPane4"); // NOI18N
        jLayeredPane4.setOpaque(true);

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "File", "Destination on Device"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jTable1.setFillsViewportHeight(true);
        jTable1.setName("jTable1"); // NOI18N
        jTable1.getTableHeader().setReorderingAllowed(false);
        jTable1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTable1MouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(jTable1);
        jTable1.getColumnModel().getColumn(0).setHeaderValue(resourceMap.getString("jTable1.columnModel.title0")); // NOI18N
        jTable1.getColumnModel().getColumn(1).setHeaderValue(resourceMap.getString("jTable1.columnModel.title1")); // NOI18N

        jScrollPane1.setBounds(20, 10, 760, 310);
        jLayeredPane4.add(jScrollPane1, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jTabbedPane1.addTab("Files to Replace", jLayeredPane4);

        jLayeredPane5.setBackground(resourceMap.getColor("jLayeredPane5.background")); // NOI18N
        jLayeredPane5.setName("jLayeredPane5"); // NOI18N
        jLayeredPane5.setOpaque(true);

        jScrollPane4.setName("jScrollPane4"); // NOI18N

        jTable2.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Image File", "Application ID"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jTable2.setFillsViewportHeight(true);
        jTable2.setName("jTable2"); // NOI18N
        jTable2.getTableHeader().setReorderingAllowed(false);
        jTable2.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTable2MouseClicked(evt);
            }
        });
        jScrollPane4.setViewportView(jTable2);

        jScrollPane4.setBounds(20, 10, 760, 310);
        jLayeredPane5.add(jScrollPane4, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jTabbedPane1.addTab("App Icons", jLayeredPane5);

        jLayeredPane6.setBackground(resourceMap.getColor("jLayeredPane6.background")); // NOI18N
        jLayeredPane6.setName("jLayeredPane6"); // NOI18N
        jLayeredPane6.setOpaque(true);

        jScrollPane5.setName("jScrollPane5"); // NOI18N

        jTable3.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Patch File"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jTable3.setFillsViewportHeight(true);
        jTable3.setName("jTable3"); // NOI18N
        jTable3.getTableHeader().setReorderingAllowed(false);
        jScrollPane5.setViewportView(jTable3);

        jScrollPane5.setBounds(20, 10, 760, 310);
        jLayeredPane6.add(jScrollPane5, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jTabbedPane1.addTab("Patches", jLayeredPane6);

        jTabbedPane1.setBounds(20, 50, 800, 360);
        jLayeredPane3.add(jTabbedPane1, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jButton5.setBackground(resourceMap.getColor("jButton5.background")); // NOI18N
        jButton5.setIcon(resourceMap.getIcon("jButton5.icon")); // NOI18N
        jButton5.setName("jButton5"); // NOI18N
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });
        jButton5.setBounds(840, 200, 40, 40);
        jLayeredPane3.add(jButton5, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jButton6.setBackground(resourceMap.getColor("jButton6.background")); // NOI18N
        jButton6.setIcon(resourceMap.getIcon("jButton6.icon")); // NOI18N
        jButton6.setIconTextGap(0);
        jButton6.setMargin(new java.awt.Insets(0, 2, 2, 2));
        jButton6.setName("jButton6"); // NOI18N
        jButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton6ActionPerformed(evt);
            }
        });
        jButton6.setBounds(840, 80, 40, 40);
        jLayeredPane3.add(jButton6, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel6.setFont(resourceMap.getFont("jLabel6.font")); // NOI18N
        jLabel6.setText(resourceMap.getString("jLabel6.text")); // NOI18N
        jLabel6.setName("jLabel6"); // NOI18N
        jLabel6.setBounds(140, 20, 160, 20);
        jLayeredPane3.add(jLabel6, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLayeredPane3.setBounds(10, 180, 900, 430);
        jLayeredPane1.add(jLayeredPane3, javax.swing.JLayeredPane.DEFAULT_LAYER);

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLayeredPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 919, Short.MAX_VALUE)
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLayeredPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 663, Short.MAX_VALUE)
        );

        menuBar.setName("menuBar"); // NOI18N

        fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        jMenuItem1.setText(resourceMap.getString("jMenuItem1.text")); // NOI18N
        jMenuItem1.setName("jMenuItem1"); // NOI18N
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        fileMenu.add(jMenuItem1);

        jSeparator1.setName("jSeparator1"); // NOI18N
        fileMenu.add(jSeparator1);

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(ca.canucksoftware.themebuilder.WebOSThemeBuilderApp.class).getContext().getActionMap(MainView.class, this);
        exitMenuItem.setAction(actionMap.get("quit")); // NOI18N
        exitMenuItem.setName("exitMenuItem"); // NOI18N
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        jMenu1.setText(resourceMap.getString("jMenu1.text")); // NOI18N
        jMenu1.setName("jMenu1"); // NOI18N

        jMenuItem4.setText(resourceMap.getString("jMenuItem4.text")); // NOI18N
        jMenuItem4.setName("jMenuItem4"); // NOI18N
        jMenuItem4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem4ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem4);

        jMenuItem3.setText(resourceMap.getString("jMenuItem3.text")); // NOI18N
        jMenuItem3.setName("jMenuItem3"); // NOI18N
        jMenuItem3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem3ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem3);

        menuBar.add(jMenu1);

        helpMenu.setText(resourceMap.getString("helpMenu.text")); // NOI18N
        helpMenu.setName("helpMenu"); // NOI18N

        aboutMenuItem.setAction(actionMap.get("showAboutBox")); // NOI18N
        aboutMenuItem.setText(resourceMap.getString("aboutMenuItem.text")); // NOI18N
        aboutMenuItem.setName("aboutMenuItem"); // NOI18N
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        setComponent(mainPanel);
        setMenuBar(menuBar);
    }// </editor-fold>//GEN-END:initComponents

    private void jTabbedPane1StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jTabbedPane1StateChanged
        if(pageLoaded) {
            jLayeredPane1.requestFocus();
            if(jTabbedPane1.getSelectedIndex()==2) {
                jButton6.setContentAreaFilled(true);
                jButton6.setBorderPainted(true);
                jButton6.setEnabled(true);
                jButton6.setText("");
                jButton6.setIcon(new ImageIcon(getClass().getResource("resources/new.png")));
            } else {
                jButton6.setContentAreaFilled(false);
                jButton6.setBorderPainted(false);
                jButton6.setEnabled(false);
                jButton6.setIcon(null);
                jButton6.setText(" ");
            }
        }
    }//GEN-LAST:event_jTabbedPane1StateChanged
    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        File f = loadFileChooser(new SSChooseFilter(), false);
        jLayeredPane1.requestFocus();
        if(f!=null) {
            ss.add(f);
            updateSSList();
        }
    }//GEN-LAST:event_jButton2ActionPerformed
    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        jLayeredPane1.requestFocus();
        ss.remove(jList2.getSelectedIndex());
        updateSSList();
    }//GEN-LAST:event_jButton1ActionPerformed
    private void jTextField5MousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTextField5MousePressed
        File f = loadFileChooser(new WallpaperChooseFilter(), false);
        jLayeredPane1.requestFocus();
        if(f!=null) {
            wallpaper = f;
            jTextField5.setText(f.getPath());
            jTextField5.setCaretPosition(jTextField5.getText().length());
        }
    }//GEN-LAST:event_jTextField5MousePressed
    private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton6ActionPerformed
        JFrame mainFrame = WebOSThemeBuilderApp.getApplication().getMainFrame();
        patchBox = new CreatePatch(mainFrame);
        patchBox.setLocationRelativeTo(mainFrame);
        patchBox.prevDir = prevDir;
        WebOSThemeBuilderApp.getApplication().show(patchBox);
        if(patchBox.patch!=null) {
            DefaultTableModel dtm = (DefaultTableModel) jTable3.getModel();
            patches.add(patchBox.patch);
            dtm.addRow(new Object[] {patchBox.patch.getPath()});
        }
    }//GEN-LAST:event_jButton6ActionPerformed
    private void jButton7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton7ActionPerformed
        JFrame mainFrame = WebOSThemeBuilderApp.getApplication().getMainFrame();
        DefaultTableModel dtm;
        if(jTabbedPane1.getSelectedIndex()==0) { //file add
            fileBox = new AddFile(mainFrame, null);
            fileBox.setLocationRelativeTo(mainFrame);
            fileBox.prevDir = prevDir;
            WebOSThemeBuilderApp.getApplication().show(fileBox);
            if(fileBox.item!=null) {
                dtm = (DefaultTableModel) jTable1.getModel();
                files.add(fileBox.item);
                dtm.addRow(new Object[] {fileBox.item.file.getPath(), fileBox.item.dest});
            }
        } else if(jTabbedPane1.getSelectedIndex()==1) { // icon add
            iconBox = new AddIcon(mainFrame, null);
            iconBox.setLocationRelativeTo(mainFrame);
            iconBox.prevDir = prevDir;
            WebOSThemeBuilderApp.getApplication().show(iconBox);
            if(iconBox.item!=null) {
                dtm = (DefaultTableModel) jTable2.getModel();
                icons.add(iconBox.item);
                dtm.addRow(new Object[] {iconBox.item.image.getPath(), iconBox.item.appID});
            }
        } else if(jTabbedPane1.getSelectedIndex()==2) { //patch add
            File f = loadFileChooser(new PatchChooseFilter(), false);
            if(f!=null) {
                dtm = (DefaultTableModel) jTable3.getModel();
                patches.add(f);
                dtm.addRow(new Object[] {f.getPath()});

            }
        }
    }//GEN-LAST:event_jButton7ActionPerformed
    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        DefaultTableModel dtm;
        int i;
        if(jTabbedPane1.getSelectedIndex()==0) { //file remove
            dtm = (DefaultTableModel) jTable1.getModel();
            i = jTable1.getSelectedRow();
            if(i>-1) {
                dtm.removeRow(i);
                files.remove(i);
            }
        } else if(jTabbedPane1.getSelectedIndex()==1) { // icon remove
            dtm = (DefaultTableModel) jTable2.getModel();
            i = jTable2.getSelectedRow();
            if(i>-1) {
                dtm.removeRow(i);
                icons.remove(i);
            }
        } else if(jTabbedPane1.getSelectedIndex()==2) { //patch remove
            dtm = (DefaultTableModel) jTable3.getModel();
            i = jTable3.getSelectedRow();
            if(i>-1) {
                LinkedList<String> p = pData.list();
                if(i<p.size()) { //premade patch
                    pData.removePatch(p.get(i));
                } else {
                    patches.remove(i-p.size());
                }
                dtm.removeRow(i);
            }
        }
    }//GEN-LAST:event_jButton5ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        String fName;
        int i;
        if(infoGood()) {
            File zip = loadFileChooser(new ZipChooseFilter(), false);
            if(zip!=null) {
                if(!zip.getName().endsWith(".zip")) {
                    File parent = zip.getParentFile();
                    String name = getFilename(zip);
                    zip = new File(parent, name + ".zip");
                }
                changeState(false);
                File xml = new File("theme.xml");
                XMLHandler theme = XMLHandler.createNewXML(xml, "data");
                TaggedElement info, previews, content, iconset, wp;
                info = (TaggedElement)Element.newElement("<themeinfo></themeinfo>");
                content = (TaggedElement)Element.newElement("<themelist></themelist>");
                theme.getRoot().add(info);
                theme.getRoot().add(content);
                info.add((TaggedElement)Element.newElement("<name>" + jTextField1.getText() + "</name>"));
                info.add((TaggedElement)Element.newElement("<version>" + jTextField2.getText() + "</version>"));
                info.add((TaggedElement)Element.newElement("<creator>" + jTextField3.getText() + "</creator>"));
                info.add((TaggedElement)Element.newElement("<description>" + jTextArea1.getText() +
                        "</description>"));
                info.add((TaggedElement)Element.newElement("<website>" + jTextField4.getText() + "</website>"));
                previews = (TaggedElement)Element.newElement("<screenshots></screenshots>");
                try {
                    ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip));
                    info.add(previews);
                    for(i=0; i<ss.size(); i++) {
                        if(ss.get(i).exists()) {
                            fName = uniqueFilename(getFilename(ss.get(i)), 0);
                            previews.add((TaggedElement)Element.newElement("<image>" + fName + "</image>"));
                            addToZip(out, ss.get(i), fName);
                        }
                    }
                    if(wallpaper!=null) {
                        fName = uniqueFilename(getFilename(wallpaper), 0);
                        wp = (TaggedElement)Element.newElement("<wallpaper></wallpaper>");
                        content.add(wp);
                        wp.add((TaggedElement)Element.newElement("<image>" + fName + "</image>"));
                        addToZip(out, wallpaper, fName);
                    }
                    for(i=0; i<files.size(); i++) {
                        if(files.get(i).file.exists()) {
                            fName = uniqueFilename(getFilename(files.get(i).file), 0);
                            TaggedElement file = (TaggedElement)Element.newElement("<file></file>");
                            content.add(file);
                            file.add((TaggedElement)Element.newElement("<filename>" + fName +
                                    "</filename>"));
                            file.add((TaggedElement)Element.newElement("<destination>" +files.get(i).dest +
                                    "</destination>"));
                            addToZip(out, files.get(i).file, fName);
                        }
                    }
                    LinkedList<File> p = pData.getPatches();
                    for(i=0; i<p.size(); i++) {
                            fName = uniqueFilename(getFilename(p.get(i)), 0);
                            TaggedElement patch = (TaggedElement)Element.newElement("<patch></patch>");
                            content.add(patch);
                            patch.add((TaggedElement)Element.newElement("<diff>" + fName + "</diff>"));
                            addToZip(out, p.get(i), fName);
                    }
                    for(i=0; i<patches.size(); i++) {
                        if(patches.get(i).exists()) {
                            fName = uniqueFilename(getFilename(patches.get(i)), 0);
                            TaggedElement patch = (TaggedElement)Element.newElement("<patch></patch>");
                            content.add(patch);
                            patch.add((TaggedElement)Element.newElement("<diff>" + fName + "</diff>"));
                            addToZip(out, patches.get(i), fName);
                        }
                    }
                    if(icons.size()>0) {
                        iconset = (TaggedElement)Element.newElement("<iconset></iconset>");
                        content.add(iconset);
                        for(i=0; i<icons.size(); i++) {
                            if(icons.get(i).image.exists()) {
                                fName = uniqueFilename(getFilename(icons.get(i).image), 0);
                                TaggedElement icon = (TaggedElement)Element.newElement("<icon></icon>");
                                iconset.add(icon);
                                icon.add((TaggedElement)Element.newElement("<appid>" + icons.get(i).appID +
                                        "</appid>"));
                                icon.add((TaggedElement)Element.newElement("<image>" + fName + "</image>"));
                                addToZip(out, icons.get(i).image, fName);
                            }
                        }
                    }
                    theme.updateFile();
                    addToZip(out, xml, "theme.xml");
                    out.flush();
                    out.close();
                    if(xml.exists())
                        xml.delete();
                } catch(Exception e) {
                    JOptionPane.showMessageDialog(mainPanel, "ERROR: " + e.getMessage());
                }
                if(zip.exists()) {
                    JOptionPane.showMessageDialog(mainPanel, "Theme zip built!");
                }
                changeState(true);
            }
        }
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        File open = loadFileChooser(new ZipChooseFilter(), false);
        if(open!=null) {
            this.changeState(false);
            t.schedule(new DoZipOpen(open), 50);
        }
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void jMenuItem4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem4ActionPerformed
        JFrame mainFrame = WebOSThemeBuilderApp.getApplication().getMainFrame();
        bulkBox = new BulkAdd(mainFrame);
        bulkBox.setLocationRelativeTo(mainFrame);
        bulkBox.prevDir = prevDir;
        WebOSThemeBuilderApp.getApplication().show(bulkBox);
        if(bulkBox.files!=null) {
            DefaultTableModel dtm = (DefaultTableModel) jTable1.getModel();
            for(int i=0; i<bulkBox.files.size(); i++) {
                FileEntry fe = new FileEntry();
                fe.file = bulkBox.files.get(i);
                fe.dest = bulkBox.dest + getFilename(fe.file);
                dtm.addRow(new Object[] {fe.file.getPath(), fe.dest});
                files.add(fe);
            }
        }
    }//GEN-LAST:event_jMenuItem4ActionPerformed

    private void jTable1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTable1MouseClicked
        int index = jTable1.getSelectedRow();
        if(evt.getClickCount()>=2  && index>-1) {
            JFrame mainFrame = WebOSThemeBuilderApp.getApplication().getMainFrame();
            fileBox = new AddFile(mainFrame, files.get(index));
            fileBox.setLocationRelativeTo(mainFrame);
            fileBox.prevDir = prevDir;
            WebOSThemeBuilderApp.getApplication().show(fileBox);
            if(fileBox.item!=null) {
                DefaultTableModel dtm = (DefaultTableModel) jTable1.getModel();
                dtm.setValueAt(fileBox.item.file.getPath(), index, 0);
                dtm.setValueAt(fileBox.item.dest, index, 1);
                files.set(index, fileBox.item);
            }
        }
    }//GEN-LAST:event_jTable1MouseClicked

    private void jTable2MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTable2MouseClicked
        int index = jTable2.getSelectedRow();
        if(evt.getClickCount()>=2  && index>-1) {
            JFrame mainFrame = WebOSThemeBuilderApp.getApplication().getMainFrame();
            iconBox = new AddIcon(mainFrame, icons.get(index));
            iconBox.setLocationRelativeTo(mainFrame);
            iconBox.prevDir = prevDir;
            WebOSThemeBuilderApp.getApplication().show(iconBox);
            if(iconBox.item!=null) {
                DefaultTableModel dtm = (DefaultTableModel) jTable2.getModel();
                dtm.setValueAt(iconBox.item.image.getPath(), index, 0);
                dtm.setValueAt(iconBox.item.appID, index, 1);
                icons.set(index, iconBox.item);
            }
        }
    }//GEN-LAST:event_jTable2MouseClicked

    private void jMenuItem3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem3ActionPerformed
        int i;
        DefaultTableModel dtm;
        JFrame mainFrame = WebOSThemeBuilderApp.getApplication().getMainFrame();
        assistant = new ThemeWizard(mainFrame, prevDir, icons, files, patches, pData, wallpaper);
        assistant.setLocationRelativeTo(mainFrame);
        WebOSThemeBuilderApp.getApplication().show(assistant);
        files = assistant.files;
        icons = assistant.icons;
        patches = assistant.patches;
        pData = assistant.pData;
        wallpaper = assistant.wallpaper;
        if(wallpaper!=null) {
            jTextField5.setText(wallpaper.getPath());
            jTextField5.setCaretPosition(jTextField5.getText().length());
        }
        dtm = (DefaultTableModel) jTable1.getModel();
        while(dtm.getRowCount()>0)
            dtm.removeRow(0);
        for(i=0; i<files.size(); i++) {
            dtm.addRow(new Object[] {files.get(i).file.getPath(), files.get(i).dest});
        }
        dtm = (DefaultTableModel) jTable2.getModel();
        while(dtm.getRowCount()>0)
            dtm.removeRow(0);
        for(i=0; i<icons.size(); i++) {
            dtm.addRow(new Object[] {icons.get(i).image.getPath(), icons.get(i).appID});
        }
        dtm = (DefaultTableModel) jTable3.getModel();
        while(dtm.getRowCount()>0)
            dtm.removeRow(0);
        for(i=0; i<patches.size(); i++) {
            dtm.addRow(new Object[] {patches.get(i).getPath()});
        }
        LinkedList<String> p = pData.list();
        for(i=0; i<p.size(); i++) {
            dtm.insertRow(i, new Object[] {p.get(i)});
        }
    }//GEN-LAST:event_jMenuItem3ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton jButton7;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JLayeredPane jLayeredPane1;
    private javax.swing.JLayeredPane jLayeredPane2;
    private javax.swing.JLayeredPane jLayeredPane3;
    private javax.swing.JLayeredPane jLayeredPane4;
    private javax.swing.JLayeredPane jLayeredPane5;
    private javax.swing.JLayeredPane jLayeredPane6;
    private javax.swing.JList jList2;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JMenuItem jMenuItem4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JTable jTable2;
    private javax.swing.JTable jTable3;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextField3;
    private javax.swing.JTextField jTextField4;
    private javax.swing.JTextField jTextField5;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JMenuBar menuBar;
    // End of variables declaration//GEN-END:variables

    private BulkAdd bulkBox;
    private ThemeWizard assistant;
    private JDialog aboutBox;
    private AddFile fileBox;
    private AddIcon iconBox;
    private CreatePatch patchBox;

    class DoDelayedLoad extends TimerTask  {
        public void run() {
            jTabbedPane1.setSelectedIndex(0);
            pageLoaded=true;
        }
    }

    class DoZipOpen extends TimerTask  {
        private File zip;
        public DoZipOpen(File f) {
            zip = f;
        }
        public void run() {
            openZip(zip);
            changeState(true);
        }
    }

    class ImagePanel extends JPanel {

        private Image img;

        public ImagePanel(String img) {
            this(new ImageIcon(img).getImage());
        }

        public ImagePanel(Image img) {
            this.img = img;
            Dimension size = new Dimension(img.getWidth(null), img.getHeight(null));
            setPreferredSize(size);
            setMinimumSize(size);
            setMaximumSize(size);
            setSize(size);
            setLayout(null);
        }

        @Override public void paintComponent(Graphics g) {
            g.drawImage(img, 0, 0, null);
        }
    }

    public class TranslucentPanel extends JLayeredPane {
        BufferedImage image = null;
        @Override
        public void paint(Graphics g) {
            if (image == null || image.getWidth() != getWidth() || image.getHeight() != getHeight()) {
                try{this.wait(500);}catch(Exception e){}
                image = (BufferedImage) createImage(getWidth(), getHeight());
            }
            Graphics2D g2 = image.createGraphics();
            g2.setClip(g.getClip());
            g2.dispose();
            g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.SrcOver.derive(0.75f));
            g2.drawImage(image, 0, 0, null);
            super.paint(g2);
            jTextField1.repaint();
            jTextField2.repaint();
            jTextField3.repaint();
            jTextField4.repaint();
            jTextArea1.repaint();
            jList2.repaint();
            jButton2.repaint();
            jButton1.repaint();
            jTextField5.repaint();
            jTabbedPane1.repaint();
            jButton6.repaint();
            jButton7.repaint();
            jButton5.repaint();
        }
    }

    class PatchChooseFilter extends javax.swing.filechooser.FileFilter {
        private final String[] okFileExtensions = new String[] {".patch", ".diff"};

        public boolean accept(File f) {
            for (String extension : okFileExtensions)
                if (f.getName().toLowerCase().endsWith(extension) || f.isDirectory())
                    return true;
            return false;
        }

        public String getDescription() {
            return "Patch Files";
        }
    }

    class WallpaperChooseFilter extends javax.swing.filechooser.FileFilter {
        private final String[] okFileExtensions = new String[] {".jpg", ".jpeg", ".png"};

        public boolean accept(File f) {
            for (String extension : okFileExtensions)
                if (f.getName().toLowerCase().endsWith(extension) || f.isDirectory())
                    return true;
            return false;
        }

        public String getDescription() {
            return "Wallpaper Files";
        }
    }

    class SSChooseFilter extends javax.swing.filechooser.FileFilter {
        private final String[] okFileExtensions = new String[] {".jpg", ".jpeg", ".png", ".gif", ".bmp"};

        public boolean accept(File f) {
            for (String extension : okFileExtensions)
                if (f.getName().toLowerCase().endsWith(extension) || f.isDirectory())
                    return true;
            return false;
        }

        public String getDescription() {
            return "Screenshot Files";
        }
    }

    class ZipChooseFilter extends javax.swing.filechooser.FileFilter {
        private final String[] okFileExtensions = new String[] {".zip"};

        public boolean accept(File f) {
            for (String extension : okFileExtensions)
                if (f.getName().toLowerCase().endsWith(extension) || f.isDirectory())
                    return true;
            return false;
        }

        public String getDescription() {
            return "Zip Files";
        }
    }
}
