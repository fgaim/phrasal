package mt.visualize.phrase;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;

//TODO Need to figure out how to remove inline paths
//and move the other paths up or down in the gridbaglayout

public class PathDialog extends JFrame {

  private static final int DEFAULT_WIDTH = 170;
  private static final int DEFAULT_HEIGHT = 300;
  private static final int BUTTON_WIDTH = 80;
  private static final int BUTTON_PANEL_HEIGHT = 60;
  private static final int MIN_PATH_PANEL_HEIGHT = 200;

  //For radio button
  private static final String ACTION_DELIM = "+";
  private static final String ON = "O";
  private static final String OFF = "F";

  //TODO Get these from the path model
  private static final String ORACLE = "o";
  private static final String ONE_BEST = "1";

  private final AnalysisDialog parent;
  private final PhraseController controller;
  private int currentTranslationId = 0;
  private int lastPathRow = 0;

  private JSplitPane mainSplitPane = null; 

  private JScrollPane pathsScrollPane = null;

  private JPanel pathsPanel = null;

  private JPanel buttonPanel = null;

  private JButton newPathButton = null;

  private JButton savePathButton = null;

  private JButton restartPathButton = null;

  private JButton finishPathButton = null;

  private JTextField userInputTextField = null;

  private final Map<Integer,Map<String,PathComponents>> pathMap;

  //Setup the font for the kill button
  private static final Font killBoxFont;
  static {
    Font f = new Font(null); //Grab the defaults
    Map fontProps = f.getAttributes();
    fontProps.put(TextAttribute.FAMILY, "SansSerif");
    fontProps.put(TextAttribute.SIZE, 14.0);
    fontProps.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
    fontProps.put(TextAttribute.FOREGROUND, Color.RED);
    killBoxFont = new Font(fontProps);
  }


  public PathDialog(AnalysisDialog parent, int currentTranslationId) {
    super();

    this.parent = parent;
    controller = PhraseController.getInstance();

    pathMap = new HashMap<Integer, Map<String,PathComponents>>();

    this.setTitle("Translation Paths");
    this.setSize(new Dimension(DEFAULT_WIDTH,DEFAULT_HEIGHT));
    this.setMinimumSize(new Dimension(DEFAULT_WIDTH,DEFAULT_HEIGHT));
    this.setContentPane(getMainSplitPane());

    this.setCurrentTranslationId(currentTranslationId);
  }

  private class PathComponents {
    public int row = -1;
    public JRadioButton on = null;
    public JRadioButton off = null;
    public NamedLabel kill = null;
    public JLabel label = null;
  }

  private JSplitPane getMainSplitPane() {
    if(mainSplitPane == null) {
      mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,getPathsScrollPane(), getButtonPanel());
      mainSplitPane.setDoubleBuffered(true);
      mainSplitPane.setResizeWeight(1.0);
    }
    return mainSplitPane;
  }

  private JScrollPane getPathsScrollPane() {
    if(pathsScrollPane == null) {
      pathsScrollPane = new JScrollPane(getPathsPanel());
      pathsScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
      //setPreferredSize
      //setMinimumSize
    }
    return pathsScrollPane;
  }

  private JPanel getPathsPanel() {
    if(pathsPanel == null) {
      pathsPanel = new JPanel(new GridBagLayout());

    }
    return pathsPanel;
  }

  private JPanel getButtonPanel() {
    if(buttonPanel == null) {
      buttonPanel = new JPanel(new GridBagLayout());
      buttonPanel.setPreferredSize(new Dimension(DEFAULT_WIDTH,BUTTON_PANEL_HEIGHT));
      GridBagConstraints c = new GridBagConstraints();
      c.fill = GridBagConstraints.HORIZONTAL;
      c.insets = new Insets(2,2,2,2);
      c.gridx = 0;
      c.gridy = 0;
      c.gridwidth = 2;
      buttonPanel.add(getUserInputTextField(),c);
      c.gridwidth = 1;
      c.gridy = 1;
      buttonPanel.add(getNewPathButton(),c);
      c.gridx = 1;
      buttonPanel.add(getSavePathButton(),c);
      c.gridx = 0;
      c.gridy = 2;
      buttonPanel.add(getRestartPathButton(),c);
      c.gridx = 1;
      buttonPanel.add(getFinishPathButton(),c);
    }
    return buttonPanel;
  }

  private JButton getNewPathButton() {
    if(newPathButton == null) {
      newPathButton = new JButton("New");
      newPathButton.setMinimumSize(new Dimension(BUTTON_WIDTH, newPathButton.getHeight()));
      newPathButton.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          toggleInputField();
        }
      });
    }
    return newPathButton;
  }

  private boolean inputFieldIsVisible = false;

  private void toggleInputField() {
    int dividerLoc = getMainSplitPane().getDividerLocation();

    getNewPathButton().setEnabled(inputFieldIsVisible);
    getSavePathButton().setEnabled(inputFieldIsVisible);
    getRestartPathButton().setEnabled(inputFieldIsVisible);
    getFinishPathButton().setEnabled(inputFieldIsVisible);
    getUserInputTextField().setVisible(!inputFieldIsVisible);
    
    if(inputFieldIsVisible) {
      getUserInputTextField().setText("");
      dividerLoc += 30;
      getMainSplitPane().setDividerLocation(dividerLoc);
    } else {
      dividerLoc -= 30;
      getMainSplitPane().setDividerLocation(dividerLoc);
      getUserInputTextField().getCaret().setVisible(true);
    }

    inputFieldIsVisible = !inputFieldIsVisible;    
  }

  private JButton getSavePathButton() {
    if(savePathButton == null) {
      savePathButton = new JButton("Save");
      savePathButton.setMinimumSize(new Dimension(BUTTON_WIDTH, savePathButton.getHeight()));
//      savePathButton.addActionListener(new ActionListener() {
//        @Override
//        public void actionPerformed(ActionEvent e) {
//          toggleInputField();
//        }
//      });
    }
    return savePathButton;
  }

  private JButton getRestartPathButton() {
    if(restartPathButton == null) {
      restartPathButton = new JButton("Restart");
      restartPathButton.setMinimumSize(new Dimension(BUTTON_WIDTH, restartPathButton.getHeight()));
    }
    return restartPathButton;
  }

  private JButton getFinishPathButton() {
    if(finishPathButton == null) {
      finishPathButton = new JButton("Finish");
      finishPathButton.setMinimumSize(new Dimension(BUTTON_WIDTH, finishPathButton.getHeight()));
    }
    return finishPathButton;
  }

  private JTextField getUserInputTextField() {
    if(userInputTextField == null) {
      userInputTextField = new JTextField();
      userInputTextField.setVisible(false);
      userInputTextField.setEditable(true);
      userInputTextField.setMinimumSize(new Dimension(DEFAULT_WIDTH - 50, 30));
      userInputTextField.setMaximumSize(new Dimension(DEFAULT_WIDTH - 50, 30));
      userInputTextField.addKeyListener(new UserEnterKeyListener());
    }
    return userInputTextField;
  }

  private class UserEnterKeyListener implements KeyListener {
    @Override
    public void keyPressed(KeyEvent e) {
      if(e.getKeyCode() == KeyEvent.VK_ENTER) {
        String newName = getUserInputTextField().getText().trim();
        if(!newName.isEmpty())
          createNewPath(newName);
        toggleInputField();
      }
    }
    public void keyReleased(KeyEvent e) {}
    public void keyTyped(KeyEvent e) {}

  }

  private boolean createNewPath(String name) {
    if(controller.addPath(currentTranslationId, name))
      addPathToPanel(name);
    else {
      System.err.printf("%s: Unable to create path (%s). Does the path name already exist?\n", this.getClass().getName(), name);
      return false;
    }

    return true;
  }

  private boolean addPathToPanel(String name) {
    //Silently return for now
    if(pathMap.get(currentTranslationId) != null && pathMap.get(this.currentTranslationId).containsKey(name))
      return false;
    
    PathComponents comps = new PathComponents();
    comps.row = lastPathRow;

    GridBagConstraints c = new GridBagConstraints();
    c.insets = new Insets(1,1,1,1);
    c.gridy = lastPathRow++;

    name = name.intern();

    c.gridx = 0;
    comps.on = new JRadioButton();
    comps.on.setActionCommand(name + ACTION_DELIM + ON);
    comps.on.addActionListener(new RadioButtonListener());
    pathsPanel.add(comps.on,c);

    (c.gridx)++;
    comps.off = new JRadioButton();
    comps.off.setActionCommand(name + ACTION_DELIM + OFF);
    comps.off.addActionListener(new RadioButtonListener());
    pathsPanel.add(comps.off,c);

    (c.gridx)++;
    comps.kill = new NamedLabel(name);
    comps.kill.setFont(killBoxFont);
    comps.kill.setText("X");
    comps.kill.setBorder(new LineBorder(Color.DARK_GRAY,1));
    comps.kill.addMouseListener(new KillButtonListener());
    pathsPanel.add(comps.kill,c);

    (c.gridx)++;
    c.insets = new Insets(1,4,1,1);
    comps.label = new JLabel(name);
    pathsPanel.add(comps.label,c);

    Map<String,PathComponents> compsMap = (pathMap.get(currentTranslationId) == null) ?
        new HashMap<String,PathComponents>() : pathMap.get(currentTranslationId);
        compsMap.put(name,comps);

    return true;
  }

  private class RadioButtonListener implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
      System.err.println(e.getActionCommand());
    }
  }

  private class KillButtonListener implements MouseListener {

    @Override
    public void mouseClicked(MouseEvent e) {
      System.out.println("Kill button");
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }
    public void mousePressed(MouseEvent e) {}
    public void mouseReleased(MouseEvent e) {}    
  }

  private boolean deletePath(String name) {
    parent.togglePath(false, name);
    controller.deletePath(currentTranslationId, name);
    //REMOVE Path from panel
    return false;
  }

  public void setCurrentTranslationId(int id) {
    currentTranslationId = id;

    //TODO Need to add functionality for switching from a previous
    //set of paths

    if(controller.getPathNames(id) != null)
      for(String name : controller.getPathNames(id))
        addPathToPanel(name);    
  }







  private static final long serialVersionUID = 6906498324826864423L;


}
