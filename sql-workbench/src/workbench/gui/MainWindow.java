/*
 * MainWindow.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import workbench.WbManager;
import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.WbConnection;
import workbench.gui.actions.AboutAction;
import workbench.gui.actions.AddMacroAction;
import workbench.gui.actions.AddTabAction;
import workbench.gui.actions.AssignWorkspaceAction;
import workbench.gui.actions.CloseWorkspaceAction;
import workbench.gui.actions.ConfigureShortcutsAction;
import workbench.gui.actions.CreateNewConnection;
import workbench.gui.actions.DataPumperAction;
import workbench.gui.actions.DisconnectTabAction;
import workbench.gui.actions.FileCloseAction;
import workbench.gui.actions.FileConnectAction;
import workbench.gui.actions.FileDisconnectAction;
import workbench.gui.actions.FileExitAction;
import workbench.gui.actions.FileNewWindowAction;
import workbench.gui.actions.FileSaveProfiles;
import workbench.gui.actions.HelpContactAction;
import workbench.gui.actions.InsertTabAction;
import workbench.gui.actions.LoadWorkspaceAction;
import workbench.gui.actions.ManageDriversAction;
import workbench.gui.actions.ManageMacroAction;
import workbench.gui.actions.NewDbExplorerPanelAction;
import workbench.gui.actions.NewDbExplorerWindowAction;
import workbench.gui.actions.NextTabAction;
import workbench.gui.actions.ObjectSearchAction;
import workbench.gui.actions.OpenFileAction;
import workbench.gui.actions.OptionsDialogAction;
import workbench.gui.actions.PrevTabAction;
import workbench.gui.actions.RemoveTabAction;
import workbench.gui.actions.RenameTabAction;
import workbench.gui.actions.SaveAsNewWorkspaceAction;
import workbench.gui.actions.SaveWorkspaceAction;
import workbench.gui.actions.SelectTabAction;
import workbench.gui.actions.ShowDbExplorerAction;
import workbench.gui.actions.ShowDbmsManualAction;
import workbench.gui.actions.ShowHelpAction;
import workbench.gui.actions.ShowMacroPopupAction;
import workbench.gui.actions.ShowManualAction;
import workbench.gui.actions.VersionCheckAction;
import workbench.gui.actions.ViewLineNumbers;
import workbench.gui.actions.ViewLogfileAction;
import workbench.gui.actions.ViewToolbarAction;
import workbench.gui.actions.WbAction;
import workbench.gui.actions.WhatsNewAction;
import workbench.gui.components.ConnectionSelector;
import workbench.gui.components.RunningJobIndicator;
import workbench.gui.components.TabCloser;
import workbench.gui.components.TabbedPaneHistory;
import workbench.gui.components.WbMenu;
import workbench.gui.components.WbTabbedPane;
import workbench.gui.components.WbToolbar;
import workbench.gui.dbobjects.DbExplorerPanel;
import workbench.gui.dbobjects.DbExplorerWindow;
import workbench.gui.fontzoom.DecreaseFontSize;
import workbench.gui.fontzoom.FontZoomer;
import workbench.gui.fontzoom.IncreaseFontSize;
import workbench.gui.fontzoom.ResetFontSize;
import workbench.gui.macros.MacroMenuBuilder;
import workbench.gui.menu.RecentWorkspaceManager;
import workbench.gui.menu.SqlTabPopup;
import workbench.gui.sql.EditorPanel;
import workbench.gui.sql.PanelType;
import workbench.gui.sql.RenameableTab;
import workbench.gui.sql.SqlPanel;
import workbench.interfaces.Connectable;
import workbench.interfaces.DbExecutionListener;
import workbench.interfaces.FilenameChangeListener;
import workbench.interfaces.MacroChangeListener;
import workbench.interfaces.MainPanel;
import workbench.interfaces.Moveable;
import workbench.interfaces.StatusBar;
import workbench.interfaces.ToolWindow;
import workbench.log.LogMgr;
import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.sql.macros.MacroManager;
import workbench.util.ExceptionUtil;
import workbench.util.FileDialogUtil;
import workbench.util.FileUtil;
import workbench.util.FileVersioner;
import workbench.util.NumberStringCache;
import workbench.util.StringUtil;
import workbench.util.WbFile;
import workbench.util.WbThread;
import workbench.util.WbWorkspace;

/**
 * The main window for the Workbench.
 * It will display several {@link workbench.gui.sql.SqlPanel}s in
 * a tabbed pane. Additionally one or more {@link workbench.gui.dbobjects.DbExplorerPanel}
 * might also be displayed inside the JTabbedPane
 *
 * @author  Thomas Kellerer
 */
public class MainWindow
	extends JFrame
	implements MouseListener, WindowListener, ChangeListener, DropTargetListener,
						MacroChangeListener, DbExecutionListener, Connectable, PropertyChangeListener,
						Moveable, RenameableTab, TabCloser
{
	private static final String DEFAULT_WORKSPACE = "Default.wksp";
	private static int instanceCount;
	private int windowId;

	private WbConnection currentConnection;
	private ConnectionProfile currentProfile;
	protected ConnectionSelector connectionSelector;

	private FileDisconnectAction disconnectAction;
	private CreateNewConnection createNewConnection;
	private DisconnectTabAction disconnectTab;
	private ShowDbExplorerAction dbExplorerAction;
	private NewDbExplorerPanelAction newDbExplorerPanel;
	private NewDbExplorerWindowAction newDbExplorerWindow;

	private WbTabbedPane sqlTab;
	private TabbedPaneHistory tabHistory;
	private WbToolbar currentToolbar;
	private List<JMenuBar> panelMenus = Collections.synchronizedList(new ArrayList<JMenuBar>(15));

	private String currentWorkspaceFile;

	private CloseWorkspaceAction closeWorkspaceAction;
	private SaveWorkspaceAction saveWorkspaceAction;
	private SaveAsNewWorkspaceAction saveAsWorkspaceAction;
	private LoadWorkspaceAction loadWorkspaceAction;
	private AssignWorkspaceAction assignWorkspaceAction;
	private NextTabAction nextTab;
	private PrevTabAction prevTab;

	private boolean resultForWorkspaceClose;

	private boolean tabRemovalInProgress;

	// will indicate a connect or disconnect in progress
	// connecting and disconnecting is done in a separate thread
	// so that slow connections do not block the GUI
	private boolean connectInProgress;

	private AddMacroAction createMacro;
	private ManageMacroAction manageMacros;
	private ShowMacroPopupAction showMacroPopup;

	private final List<ToolWindow> explorerWindows = new ArrayList<ToolWindow>();

	private RunningJobIndicator jobIndicator;
	protected WbThread connectThread;

	public MainWindow()
	{
		super(ResourceMgr.TXT_PRODUCT_NAME);

		// Control the brushed metal look for MacOS, this must be set as soon as possible on the
		// root pane in order to have an effect
		getRootPane().putClientProperty("apple.awt.brushMetalLook", GuiSettings.getUseBrushedMetal());

		this.windowId = ++instanceCount;

		sqlTab = new WbTabbedPane();
		tabHistory = new TabbedPaneHistory(sqlTab);

		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		// There is no need to register the actions with the ActionMap
		// as they will be handed over to the FocusManager in windowActivated()
		nextTab = new NextTabAction(sqlTab);
		prevTab = new PrevTabAction(sqlTab);

		initMenu();

		ResourceMgr.setMainWindowIcons(this);

		getContentPane().add(this.sqlTab, BorderLayout.CENTER);

		restoreSettings();

		updateTabPolicy();

		sqlTab.addChangeListener(this);
		sqlTab.addMouseListener(this);
		sqlTab.hideDisabledButtons(false);
		if (GuiSettings.getShowSqlTabCloseButton())
		{
			sqlTab.showCloseButton(this);
		}

		addWindowListener(this);

		MacroManager.getInstance().getMacros().addChangeListener(this);

		new DropTarget(this.sqlTab, DnDConstants.ACTION_COPY, this);
		sqlTab.enableDragDropReordering(this);

		Settings.getInstance().addPropertyChangeListener(this,
			Settings.PROPERTY_SHOW_TOOLBAR,
			Settings.PROPERTY_SHOW_TAB_INDEX,
			GuiSettings.PROPERTY_SQLTAB_CLOSE_BUTTON,
			Settings.PROPERTY_TAB_POLICY
		);
	}

	protected final void updateTabPolicy()
	{
		int tabPolicy = Settings.getInstance().getIntProperty(Settings.PROPERTY_TAB_POLICY, JTabbedPane.WRAP_TAB_LAYOUT);
		this.sqlTab.setTabLayoutPolicy(tabPolicy);
	}

	public void display()
	{
		this.restoreState();
		this.setVisible(true);
		this.addTab(true, false, true, false);
		this.updateGuiForTab(0);
		this.updateWindowTitle();

		boolean macroVisible = Settings.getInstance().getBoolProperty(this.getClass().getName() + ".macropopup.visible", false);
		if (macroVisible)
		{
			EventQueue.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					showMacroPopup.showPopup();
				}
			});
		}
	}

	/**
	 * The listener will be notified when the name of a tab changes.
	 * This is used in the {@link workbench.gui.dbobjects.TableListPanel}
	 * to display the available panels in the context menu
	 * @see workbench.gui.dbobjects.EditorTabSelectMenu#fileNameChanged(Object, String)
	 */
	public void addFilenameChangeListener(FilenameChangeListener aListener)
	{
		for (int i=0; i < this.sqlTab.getTabCount(); i++)
		{
			MainPanel panel = this.getSqlPanel(i);
			if (panel instanceof SqlPanel)
			{
				SqlPanel sql = (SqlPanel)panel;
				sql.addFilenameChangeListener(aListener);
			}
		}
	}

	/**
	 * Remove the file name change listener.
	 * @see #addFilenameChangeListener(FilenameChangeListener )
	 */
	public void removeFilenameChangeListener(FilenameChangeListener aListener)
	{
		for (int i=0; i < this.sqlTab.getTabCount(); i++)
		{
			MainPanel panel = this.getSqlPanel(i);
			if (panel instanceof SqlPanel)
			{
				SqlPanel sql = (SqlPanel)panel;
				sql.removeFilenameChangeListener(aListener);
			}
		}
	}

	/**
	 * The listener will be notified when the current tab changes.
	 * This is used in the {@link workbench.gui.dbobjects.TableListPanel}
	 * to highlight the current tab the context menu
	 * @see workbench.gui.dbobjects.TableListPanel#stateChanged(ChangeEvent)
	 */
	@Override
	public void addTabChangeListener(ChangeListener aListener)
	{
		this.sqlTab.addChangeListener(aListener);
	}

	public void removeIndexChangeListener(ChangeListener aListener)
	{
		this.sqlTab.removeChangeListener(aListener);
	}

	public void addExecutionListener(DbExecutionListener l)
	{
		int count = this.sqlTab.getTabCount();
		for (int i = 0; i < count; i++)
		{
			MainPanel p = this.getSqlPanel(i);
			if (p instanceof SqlPanel)
			{
				((SqlPanel)p).addDbExecutionListener(l);
			}
		}
	}

	public void removeExecutionListener(DbExecutionListener l)
	{
		int count = this.sqlTab.getTabCount();
		for (int i = 0; i < count; i++)
		{
			MainPanel p = this.getSqlPanel(i);
			if (p instanceof SqlPanel)
			{
				((SqlPanel)p).removeDbExecutionListener(l);
			}
		}
	}

	protected void checkWorkspaceActions()
	{
		this.saveWorkspaceAction.setEnabled(this.currentWorkspaceFile != null);
		this.assignWorkspaceAction.setEnabled(this.currentWorkspaceFile != null && this.currentProfile != null);
		this.closeWorkspaceAction.setEnabled(this.currentWorkspaceFile != null);
	}

	private void initMenu()
	{
		this.disconnectAction = new FileDisconnectAction(this);
		this.assignWorkspaceAction = new AssignWorkspaceAction(this);
		this.closeWorkspaceAction = new CloseWorkspaceAction(this);
		this.saveAsWorkspaceAction = new SaveAsNewWorkspaceAction(this);

		this.createNewConnection = new CreateNewConnection(this);
		this.disconnectTab = new DisconnectTabAction(this);

		this.loadWorkspaceAction = new LoadWorkspaceAction(this);
		this.saveWorkspaceAction = new SaveWorkspaceAction(this);

		this.createMacro = new AddMacroAction();
		this.manageMacros = new ManageMacroAction(this);
		showMacroPopup = new ShowMacroPopupAction(this);

		this.dbExplorerAction = new ShowDbExplorerAction(this);
		this.newDbExplorerPanel = new NewDbExplorerPanelAction(this);
		this.newDbExplorerWindow = new NewDbExplorerWindowAction(this);

		int tabCount = this.sqlTab.getTabCount();
		for (int tab=0; tab < tabCount; tab ++)
		{
			MainPanel sql = (MainPanel)this.sqlTab.getComponentAt(tab);
			JMenuBar menuBar = this.createMenuForPanel(sql);
			this.panelMenus.add(menuBar);
		}
	}

	private JMenuBar createMenuForPanel(MainPanel panel)
	{
		HashMap<String, JMenu> menus = new HashMap<String, JMenu>(10);

		JMenuBar menuBar = new JMenuBar();
		menuBar.setBorderPainted(false);
		menuBar.putClientProperty("jgoodies.headerStyle", "Single");

		// Create the file menu for all tabs
		JMenu menu = new WbMenu(ResourceMgr.getString(ResourceMgr.MNU_TXT_FILE));
		menu.setName(ResourceMgr.MNU_TXT_FILE);
		menuBar.add(menu);
		menus.put(ResourceMgr.MNU_TXT_FILE, menu);

		WbAction action;

		action = new FileConnectAction(this);
		action.addToMenu(menu);
		this.disconnectAction.addToMenu(menu);
		FileCloseAction close = new FileCloseAction(this);
		close.addToMenu(menu);
		menu.addSeparator();
		this.createNewConnection.addToMenu(menu);
		this.disconnectTab.addToMenu(menu);
		menu.addSeparator();

		action = new FileSaveProfiles();
		action.addToMenu(menu);

		action = new FileNewWindowAction();
		action.addToMenu(menu);

		OpenFileAction open = new OpenFileAction(this);
		menu.addSeparator();
		open.addToMenu(menu);

		// now create the menus for the current tab
		List actions = panel.getActions();

		// Create the menus in the correct order
		if (panel instanceof SqlPanel)
		{
			menu = new WbMenu(ResourceMgr.getString(ResourceMgr.MNU_TXT_EDIT));
			menu.setName(ResourceMgr.MNU_TXT_EDIT);
			menu.setVisible(false);
			menuBar.add(menu);
			menus.put(ResourceMgr.MNU_TXT_EDIT, menu);
		}

		menu = new WbMenu(ResourceMgr.getString(ResourceMgr.MNU_TXT_VIEW));
		menu.setName(ResourceMgr.MNU_TXT_VIEW);
		menu.setVisible(true);
		menuBar.add(menu);
		menus.put(ResourceMgr.MNU_TXT_VIEW, menu);

		int tabCount = this.sqlTab.getTabCount();
		for (int i=0; i < tabCount; i ++)
		{
			action = new SelectTabAction(this.sqlTab, i);
			menu.add(action);
		}
		menu.addSeparator();
		menu.add(nextTab.getMenuItem());
		menu.add(prevTab.getMenuItem());

		if (panel instanceof SqlPanel)
		{
			menu = new WbMenu(ResourceMgr.getString(ResourceMgr.MNU_TXT_DATA));
			menu.setName(ResourceMgr.MNU_TXT_DATA);
			menu.setVisible(false);
			menuBar.add(menu);
			menus.put(ResourceMgr.MNU_TXT_DATA, menu);

			menu = new WbMenu(ResourceMgr.getString(ResourceMgr.MNU_TXT_SQL));
			menu.setName(ResourceMgr.MNU_TXT_SQL);
			menu.setVisible(false);
			menuBar.add(menu);
			menus.put(ResourceMgr.MNU_TXT_SQL, menu);

			final WbMenu macroMenu = new WbMenu(ResourceMgr.getString(ResourceMgr.MNU_TXT_MACRO));
			macroMenu.setName(ResourceMgr.MNU_TXT_MACRO);
			macroMenu.setVisible(true);
			menuBar.add(macroMenu);
			menus.put(ResourceMgr.MNU_TXT_MACRO, macroMenu);
			buildMacroMenu(macroMenu);
		}

		menu = new WbMenu(ResourceMgr.getString(ResourceMgr.MNU_TXT_WORKSPACE));
		menu.setName(ResourceMgr.MNU_TXT_WORKSPACE);
		menuBar.add(menu);
		menus.put(ResourceMgr.MNU_TXT_WORKSPACE, menu);
		menu.add(this.saveWorkspaceAction);
		menu.add(this.saveAsWorkspaceAction);
		menu.add(this.loadWorkspaceAction);
		menu.addSeparator();
		menu.add(this.closeWorkspaceAction);
		menu.add(this.assignWorkspaceAction);
		menu.addSeparator();
		JMenu recentWorkspace = new JMenu(ResourceMgr.getString("MnuTxtRecentWorkspace"));
		recentWorkspace.setName("recent-workspace");
		RecentWorkspaceManager.getInstance().populateMenu(recentWorkspace, this);
		menu.add(recentWorkspace);

		WbMenu submenu = null;
		String menuName = null;
		for (int i=0; i < actions.size(); i++)
		{
			submenu = null;
			action = null;
			menuName = null;
			Object entry = actions.get(i);
			boolean menuSep = false;
			if (entry instanceof WbAction)
			{
				action = (WbAction)actions.get(i);
				menuName = action.getMenuItemName();
				menuSep = action.getCreateMenuSeparator();
			}
			else if (entry instanceof WbMenu)
			{
				submenu = (WbMenu)entry;
				menuName = submenu.getParentMenuId();
				menuSep = submenu.getCreateMenuSeparator();
			}

			if (menuName == null)
			{
				LogMgr.logWarning(this, "Action " + action.getClass() + " does not define a main menu entry!");
				continue;
			}
			menu = menus.get(menuName);
			if (menu == null)
			{
				menu = new WbMenu(ResourceMgr.getString(menuName));
				menuBar.add(menu);
				menus.put(menuName, menu);
			}

			if (menuSep)
			{
				menu.addSeparator();
			}

			if (action != null)
			{
				action.addToMenu(menu);
			}
			else if (submenu != null)
			{
				menu.add(submenu);
			}
			menu.setVisible(true);
		}

		final JMenu filemenu = menus.get(ResourceMgr.MNU_TXT_FILE);
		filemenu.addSeparator();
		filemenu.add(new ManageDriversAction());
		filemenu.addSeparator();

		action = new FileExitAction();
		filemenu.add(action);

		final JMenu viewMenu = menus.get(workbench.resource.ResourceMgr.MNU_TXT_VIEW);
		AddTabAction add = new AddTabAction(this);
		viewMenu.addSeparator();
		viewMenu.add(add);
		InsertTabAction insert = new InsertTabAction(this);
		viewMenu.add(insert);

		RemoveTabAction rem = new RemoveTabAction(this);
		viewMenu.add(rem);
		viewMenu.add(new RenameTabAction(this));
		viewMenu.addSeparator();
		ViewLineNumbers v = new ViewLineNumbers();
		v.addToMenu(viewMenu);

		WbAction vTb = new ViewToolbarAction();
		vTb.addToMenu(viewMenu);

		if (panel instanceof SqlPanel)
		{
			JMenu zoom = new JMenu(ResourceMgr.getString("TxtZoom"));
			SqlPanel sqlpanel = (SqlPanel)panel;
			EditorPanel editor = sqlpanel.getEditor();
			FontZoomer zoomer = editor.getFontZoomer();

			IncreaseFontSize inc = new IncreaseFontSize(zoomer);
			DecreaseFontSize dec = new DecreaseFontSize(zoomer);
			ResetFontSize reset = new ResetFontSize(zoomer);

			zoom.add(new JMenuItem(inc));
			zoom.add(new JMenuItem(dec));
			zoom.addSeparator();
			zoom.add(new JMenuItem(reset));
			viewMenu.add(zoom);
		}

		menuBar.add(this.buildToolsMenu());
		menuBar.add(this.buildHelpMenu());

		panel.addToToolbar(this.dbExplorerAction, true);
		return menuBar;
	}

	/**
	 * Removes or makes the toolbar visible depending on
	 * {@link GuiSettings#getShowToolbar}.
	 *
	 * This method will <i>validate</i> this' {@link #getContentPane content pane}
	 * in case a change on the toolbar's visibility is performed.
	 *
	 * This method should be called on the EDT.
	 */
	private void updateToolbarVisibility()
	{
		final Container content = this.getContentPane();

		if (this.currentToolbar != null)
		{
			content.remove(this.currentToolbar);
			this.currentToolbar = null;
		}

		if (GuiSettings.getShowToolbar())
		{
			final MainPanel curPanel = this.getCurrentPanel();
			if (curPanel != null)
			{
				this.currentToolbar = curPanel.getToolbar();
				content.add(currentToolbar, BorderLayout.NORTH);
			}
		}
		content.invalidate();
	}

	protected void forceRedraw()
	{
		Container content = this.getContentPane();
		content.invalidate();
		validate();
		doLayout();
		WbSwingUtilities.repaintLater(this);
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		if (Settings.PROPERTY_SHOW_TOOLBAR.equals(evt.getPropertyName()))
		{
			this.updateToolbarVisibility();
			forceRedraw();
		}
		else if (Settings.PROPERTY_SHOW_TAB_INDEX.equals(evt.getPropertyName()))
		{
			this.renumberTabs();
		}
		else if (GuiSettings.PROPERTY_SQLTAB_CLOSE_BUTTON.equals(evt.getPropertyName()))
		{
			if (GuiSettings.getShowSqlTabCloseButton())
			{
				sqlTab.showCloseButton(this);
			}
			else
			{
				sqlTab.showCloseButton(null);
			}
		}
		else if (Settings.PROPERTY_TAB_POLICY.equals(evt.getPropertyName()))
		{
			updateTabPolicy();
		}
	}

	private void checkMacroMenuForPanel(int index)
	{
		MainPanel p = this.getSqlPanel(index);
		try
		{
			JMenu macro = this.getMacroMenu(index);
			setMacroMenuItemStates(macro, p.isConnected());
		}
		catch (Exception e)
		{
			LogMgr.logError("MainWindow.checkMacroMenuForPanel()", "Error during macro update", e);
		}
	}

	private void setMacroMenuEnabled(boolean enabled)
	{
		int count = this.sqlTab.getTabCount();
		for (int i=0; i < count; i++)
		{
			JMenu macro = this.getMacroMenu(i);
			setMacroMenuItemStates(macro, enabled);
		}
	}

	private void setMacroMenuItemStates(JMenu menu, boolean enabled)
	{
		if (menu != null)
		{
			int itemCount = menu.getItemCount();

			// The actual macro entries start at index 3
			// 0,1,2 are menu items,
			for (int in=3; in < itemCount; in++)
			{
				JMenuItem item = menu.getItem(in);
				if (item != null) item.setEnabled(enabled);
			}
		}
	}

	@Override
	public void macroListChanged()
	{
		int count = this.sqlTab.getTabCount();
		for (int i=0; i < count; i++)
		{
			JMenu macros = this.getMacroMenu(i);
			if (macros != null)
			{
				this.buildMacroMenu(macros);
				MainPanel p = this.getSqlPanel(i);
				this.setMacroMenuItemStates(macros, p.isConnected());
			}
		}
	}

	private void buildMacroMenu(JMenu macroMenu)
	{
		macroMenu.removeAll();
		createMacro.addToMenu(macroMenu);
		manageMacros.addToMenu(macroMenu);
		showMacroPopup.addToMenu(macroMenu);

		MacroMenuBuilder builder = new MacroMenuBuilder();
		builder.buildMacroMenu(this, macroMenu);
	}

	public int getCurrentPanelIndex()
	{
		return this.sqlTab.getSelectedIndex();
	}

	public int getIndexForPanel(MainPanel panel)
	{
		int tabCount = this.sqlTab.getTabCount();
		for (int i=0; i < tabCount; i++)
		{
			MainPanel p = this.getSqlPanel(i);
			if (p.getId().equals(panel.getId())) return i;
		}
		return -1;
	}

	/**
	 * Return a list of titles for all sql panels.
	 * For indexes where a DbExplorer is open a NULL string will be returned
	 * at that index position in the list.
	 */
	public List<String> getPanelLabels()
	{
		int tabCount = this.sqlTab.getTabCount();

		List<String> result = new ArrayList<String>(tabCount);
		for (int i=0; i < tabCount; i++)
		{
			MainPanel p = this.getSqlPanel(i);
			if (p instanceof SqlPanel)
			{
				result.add(p.getTabTitle());
			}
			else
			{
				result.add(null);
			}
		}
		return result;
	}

	public MainPanel getCurrentPanel()
	{
		int index = this.sqlTab.getSelectedIndex();
		if (index >-1) return this.getSqlPanel(index);
		else return null;
	}

	public SqlPanel getCurrentSqlPanel()
	{
		MainPanel p = this.getCurrentPanel();
		if (p instanceof SqlPanel)
		{
			return (SqlPanel)p;
		}
		return null;
	}

	public int getTabCount()
	{
		return this.sqlTab.getTabCount();
	}

	public MainPanel getSqlPanel(int anIndex)
	{
		try
		{
			return (MainPanel)this.sqlTab.getComponentAt(anIndex);
		}
		catch (Exception e)
		{
			LogMgr.logDebug("MainWindow.getSqlPanel()", "Invalid index [" + anIndex + "] specified!", e);
			return null;
		}
	}

	public void selectTab(int anIndex)
	{
		this.sqlTab.setSelectedIndex(anIndex);
	}

	private boolean isConnectInProgress()
	{
		return this.connectInProgress;
	}

	private void clearConnectIsInProgress()
	{
		this.connectInProgress = false;
	}

	private void setConnectIsInProgress()
	{
		this.connectInProgress = true;
	}

	private void checkConnectionForPanel(final MainPanel aPanel)
	{
		if (this.isConnectInProgress()) return;
		if (aPanel.isConnected()) return;

		try
		{
			if (this.currentProfile != null && this.currentProfile.getUseSeparateConnectionPerTab())
			{
				createNewConnectionForPanel(aPanel);
			}
			else if (this.currentConnection != null)
			{
				aPanel.setConnection(this.currentConnection);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("MainWindow.checkConnectionForPanel()", "Error when checking connection", e);
		}
	}

	public void disconnectCurrentPanel()
	{
		if (this.currentProfile == null) return;
		if (this.currentProfile.getUseSeparateConnectionPerTab()) return;

		final MainPanel p = this.getCurrentPanel();
		WbConnection con = p.getConnection();
		if (con == this.currentConnection) return;

		Thread t = new WbThread("Disconnect panel " + p.getId())
		{
			@Override
			public void run()
			{
				disconnectPanel(p);
			}
		};
		t.start();
	}

	protected void disconnectPanel(final MainPanel panel)
	{
		if (this.isConnectInProgress()) return;
		boolean inProgress = isConnectInProgress();
		if (!inProgress) setConnectIsInProgress();

		showDisconnectInfo();
		showStatusMessage(ResourceMgr.getString("MsgDisconnecting"));
		try
		{
			WbConnection old = panel.getConnection();
			panel.disconnect();
			ConnectionMgr.getInstance().disconnect(old);
			panel.setConnection(currentConnection);
			int index = this.getIndexForPanel(panel);
			sqlTab.setForegroundAt(index, null);
		}
		catch (Throwable e)
		{
			LogMgr.logError("MainWindow.connectPanel()", "Error when disconnecting panel " + panel.getId(), e);
			String error = ExceptionUtil.getDisplay(e);
			WbSwingUtilities.showErrorMessage(this, error);
		}
		finally
		{
			showStatusMessage("");
			closeConnectingInfo();
			if (!inProgress) clearConnectIsInProgress();
		}

		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				createNewConnection.checkState();
				disconnectTab.checkState();
			}
		});
	}

	public boolean canUseSeparateConnection()
	{
		if (this.currentProfile == null) return false;
		return !this.currentProfile.getUseSeparateConnectionPerTab();
	}

	public boolean usesSeparateConnection()
	{
		if (!canUseSeparateConnection()) return false;
		final MainPanel current = this.getCurrentPanel();
		WbConnection conn = current.getConnection() ;

		return (currentConnection != null && conn != this.currentConnection);
	}

	public void createNewConnectionForCurrentPanel()
	{
		final MainPanel panel = getCurrentPanel();
		createNewConnectionForPanel(panel);
		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				int index = getIndexForPanel(panel);
				sqlTab.setForegroundAt(index, Color.BLUE);
			}
		});
	}

	protected void createNewConnectionForPanel(final MainPanel aPanel)
	{
		if (this.isConnectInProgress()) return;
		if (this.connectThread != null) return;

		this.showConnectingInfo();

		this.connectThread = new WbThread("Panel Connect " + aPanel.getId())
		{
			@Override
			public void run()
			{
				connectPanel(aPanel);
			}
		};
		this.connectThread.start();
	}

	/**
	 * Connect the given panel to the database. This will always
	 * create a new physical connection to the database.
	 */
	protected void connectPanel(final MainPanel aPanel)
	{
		if (this.isConnectInProgress()) return;
		this.setConnectIsInProgress();

		try
		{
			// prevent a manual tab change during connection as this is not working properly
			sqlTab.setEnabled(false);

			WbConnection conn = this.getConnectionForTab(aPanel, true);
			int index = this.getIndexForPanel(aPanel);
			this.tabConnected(aPanel, conn, index);
		}
		catch (Throwable e)
		{
			LogMgr.logError("MainWindow.connectPanel()", "Error when connecting panel " + aPanel.getId(), e);
			showStatusMessage("");
			String error = ExceptionUtil.getDisplay(e);
			String msg = ResourceMgr.getFormattedString("ErrConnectFailed", error.trim());
			WbSwingUtilities.showErrorMessage(this, msg);
		}
		finally
		{
			sqlTab.setEnabled(true);
			closeConnectingInfo();
			clearConnectIsInProgress();
			this.connectThread = null;
		}
	}

	public void waitForConnection()
	{
		if (this.connectThread != null)
		{
			try
			{
				this.connectThread.join();
			}
			catch (Exception e)
			{
				LogMgr.logError("MainWindow.waitForConnection()", "Error joining connection thread", e);
			}
		}
	}

	private void tabConnected(final MainPanel panel, WbConnection conn, final int anIndex)
	{
		this.closeConnectingInfo();
		panel.setConnection(conn);

		WbSwingUtilities.waitForEmptyQueue();
		WbSwingUtilities.invoke(new Runnable()
		{
			@Override
			public void run()
			{
				updateGuiForTab(anIndex);
			}
		});
	}

	private synchronized void updateGuiForTab(int index)
	{
		if (index < 0) return;

		final MainPanel current = this.getSqlPanel(index);
		if (current == null) return;

		JMenuBar menu = this.panelMenus.get(index);
		if (menu == null)	return;

		this.setJMenuBar(menu);

		this.updateToolbarVisibility();

		this.createNewConnection.checkState();
		this.disconnectTab.checkState();

		this.checkMacroMenuForPanel(index);

		forceRedraw();

		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				current.panelSelected();
			}
		});
	}

	private void tabSelected(final int index)
	{
		if (index < 0) return;
		if (index >= sqlTab.getTabCount()) return;

		// Make sure this is executed on the EDT
		WbSwingUtilities.invoke(new Runnable()
		{
			@Override
			public void run()
			{
				updateCurrentTab(index);
			}
		});
	}

	private void updateCurrentTab(int index)
	{
		MainPanel current = getSqlPanel(index);
		if (current == null) return;
		this.checkConnectionForPanel(current);
		this.updateGuiForTab(index);
		this.updateAddMacroAction();
		this.updateWindowTitle();
	}

	protected void updateAddMacroAction()
	{
		SqlPanel sql = this.getCurrentSqlPanel();
		if (sql != null)
		{
			this.createMacro.setClient(sql.getEditor());
		}
	}

	public void restoreState()
	{
		String state = Settings.getInstance().getProperty(this.getClass().getName() + ".state", "0");
		int i = 0;
		try { i = Integer.parseInt(state); } catch (Exception e) { i = 0; }
		if (i == MAXIMIZED_BOTH)
		{
			this.setExtendedState(i);
		}
	}

	public final void restoreSettings()
	{
		Settings s = Settings.getInstance();

		if (!s.restoreWindowSize(this))
		{
			this.setSize(950,750);
		}

		if (!s.restoreWindowPosition(this))
		{
			WbSwingUtilities.center(this, null);
		}
	}

	public void saveSettings()
	{
		Settings sett = Settings.getInstance();
		int state = this.getExtendedState();
		sett.setProperty(this.getClass().getName() + ".state", state);

		if (state != MAXIMIZED_BOTH)
		{
			sett.storeWindowPosition(this);
			sett.storeWindowSize(this);
		}
		boolean macroVisible = (showMacroPopup != null && showMacroPopup.isPopupVisible());
		sett.setProperty(this.getClass().getName() + ".macropopup.visible", macroVisible);
	}

	@Override
	public void windowOpened(WindowEvent windowEvent)
	{
	}

	@Override
	public void windowClosed(WindowEvent e)
	{
	}

	@Override
	public void windowDeiconified(WindowEvent windowEvent)
	{
	}

	@Override
	public void windowClosing(WindowEvent windowEvent)
	{
		WbManager.getInstance().closeMainWindow(this);
	}

	@Override
	public void windowDeactivated(WindowEvent windowEvent)
	{
		WbFocusManager.getInstance().grabActions(null, null);
	}

	@Override
	public void windowActivated(WindowEvent windowEvent)
	{
		WbFocusManager.getInstance().grabActions(nextTab, prevTab);
	}

	@Override
	public void windowIconified(WindowEvent windowEvent)
	{
	}

	/**
	 *	Display a message in the status bar
	 */
	public void showStatusMessage(final String aMsg)
	{
		final MainPanel current = this.getCurrentPanel();
		if (current == null) return;

		WbSwingUtilities.invoke(new Runnable()
		{
			@Override
			public void run()
			{
				if (StringUtil.isEmptyString(aMsg))
				{
					current.clearStatusMessage();
				}
				else
				{
					current.showStatusMessage(aMsg);
				}
			}
		});
	}

	public void showLogMessage(String aMsg)
	{
		MainPanel current = this.getCurrentPanel();
		if (current != null) current.showLogMessage(aMsg);
	}

	@Override
	public boolean connectBegin(final ConnectionProfile aProfile, final StatusBar info)
	{
		if (this.isBusy() || this.isCancelling())
		{
			WbSwingUtilities.showErrorMessageKey(this, "MsgDisconnectBusy");
			return false;
		}

		if (this.currentWorkspaceFile != null && WbManager.getInstance().getSettingsShouldBeSaved())
		{
			if (!this.saveWorkspace(this.currentWorkspaceFile, true))
			{
				return false;
			}
		}

		if (this.isConnected())
		{
			showDisconnectInfo();
		}
		disconnect(false, false, false);

		// it is important to set the connectInProgress flag,
		// otherwise loading the workspace will already trigger a
		// panel switch which might cause a connect
		// to the current profile before the ConnectionSelector
		// has actually finished.
		// this has to be set AFTER calling disconnect(), because
		// disconnect respects this flag and does nothing...
		this.setConnectIsInProgress();

		this.currentProfile = aProfile;

		showStatusMessage(ResourceMgr.getString("MsgLoadingWorkspace"));
		if (info != null) info.setStatusMessage(ResourceMgr.getString("MsgLoadingWorkspace"));
		loadWorkspaceForProfile(currentProfile);
		Settings.getInstance().setLastConnection(currentProfile);
		showStatusMessage(ResourceMgr.getString("MsgConnecting"));
		return true;
	}

	public String getWindowId()
	{
		return NumberStringCache.getNumberString(windowId);
	}

	private String getConnectionIdForPanel(MainPanel p)
	{
		if (p == null)
		{
			LogMgr.logError("MainWindow.getConnectionIdForPanel()", "Requested connection ID for NULL panel!", new Exception());
			return "Wb" + getWindowId();
		}
		return "Wb" + getWindowId() + "-" + p.getId();
	}

	/**
	 * Return the internal ID that should be used when connecting
	 * to the given connection profile
	 * @return an id specific for the current tab or a "global" id the connection
	 *         is shared between all tabs of this window
	 */
	@Override
	public String getConnectionId(ConnectionProfile aProfile)
	{
		if (aProfile != null && aProfile.getUseSeparateConnectionPerTab())
		{
			return getConnectionIdForPanel(this.getCurrentPanel());
		}
		else
		{
			return "WbWin-" + getWindowId();
		}
	}

	private ConnectionSelector getSelector()
	{
		if (connectionSelector == null)
		{
			connectionSelector = new ConnectionSelector(this, this);
		}
		return connectionSelector;
	}

	public void connectTo(ConnectionProfile profile, boolean showDialog)
	{
		getSelector().connectTo(profile, showDialog);
	}


	/**
	 *	Call-back function which gets executed on the AWT thread after
	 *  the initial connection has been completed
	 */
	@Override
	public void connected(WbConnection conn)
	{
		if (this.currentProfile.getUseSeparateConnectionPerTab())
		{
			this.getCurrentPanel().setConnection(conn);
		}
		else
		{
			this.setConnection(conn);
		}

		this.setMacroMenuEnabled(true);
		this.updateWindowTitle();

		this.dbExplorerAction.setEnabled(true);
		this.newDbExplorerPanel.setEnabled(true);
		this.newDbExplorerWindow.setEnabled(true);

		this.disconnectAction.setEnabled(true);
		this.createNewConnection.checkState();
		this.disconnectTab.checkState();
		this.getCurrentPanel().clearLog();
		this.getCurrentPanel().showResultPanel();
		DatabaseMetaData meta = conn.getMetadata().getJdbcMetaData();
		try
		{
			int major = meta.getDatabaseMajorVersion();
			int minor = meta.getDatabaseMinorVersion();
			ShowDbmsManualAction.getInstance().setDbms(conn.getMetadata().getDbId(), major, minor);
		}
		catch (SQLException sql)
		{
			ShowDbmsManualAction.getInstance().setDbms(conn.getMetadata().getDbId(), -1, -1);
		}
		showConnectionWarnings(conn, this.getCurrentPanel());
		selectCurrentEditor();
	}

	@Override
	public void connectFailed(String error)
	{
		disconnected(true);
		tabSelected(0);

		if (error == null) return;

		try
		{
			String msg = ResourceMgr.getFormattedString("ErrConnectFailed", error.trim());
			if (error.indexOf('\n') > 0 || error.indexOf('\r') > 0)
			{
				WbSwingUtilities.showMultiLineError(this, msg);
			}
			else
			{
				WbSwingUtilities.showErrorMessage(this, msg);
			}
		}
		catch (Throwable th)
		{
			LogMgr.logError("MainWindow.connectFailed()", "Could not display connection error!", th);
			WbSwingUtilities.showErrorMessage(this, error);
		}
	}

	@Override
	public void connectCancelled()
	{
		if (this.exitOnCancel)
		{
			WbManager.getInstance().closeMainWindow(this);
		}
	}

	@Override
	public void connectEnded()
	{
		for (int i=0; i < sqlTab.getTabCount(); i++)
		{
			MainPanel sql = getSqlPanel(i);
			sql.clearStatusMessage();
		}
		this.clearConnectIsInProgress();
	}

	private static final int CREATE_WORKSPACE = 0;
	private static final int LOAD_OTHER_WORKSPACE = 1;
	private static final int IGNORE_MISSING_WORKSPACE = 2;

	private int checkNonExistingWorkspace()
	{
		String[] options = new String[] { ResourceMgr.getString("LblCreateWorkspace"), ResourceMgr.getString("LblLoadWorkspace"), ResourceMgr.getString("LblIgnore")};
		JOptionPane ignorePane = new JOptionPane(ResourceMgr.getString("MsgProfileWorkspaceNotFound"), JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_CANCEL_OPTION, null, options);
		JDialog dialog = ignorePane.createDialog(this, ResourceMgr.TXT_PRODUCT_NAME);
		try
		{
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setResizable(true);
			dialog.pack();
			dialog.setVisible(true);
		}
		finally
		{
			dialog.dispose();
		}
		Object result = ignorePane.getValue();
		if (result == null) return CREATE_WORKSPACE;
		else if (result.equals(options[0])) return CREATE_WORKSPACE;
		else if (result.equals(options[1])) return LOAD_OTHER_WORKSPACE;
		else return IGNORE_MISSING_WORKSPACE;
	}

	private void handleWorkspaceLoadError(Throwable e, String realFilename)
	{
		String error = ExceptionUtil.getDisplay(e);
		String msg = StringUtil.replace(ResourceMgr.getString("ErrLoadingWorkspace"), "%error%", error);
		if (e instanceof OutOfMemoryError)
		{
			msg = ResourceMgr.getString("MsgOutOfMemoryError");
		}
		boolean create = WbSwingUtilities.getYesNo(this, msg);
		if (create)
		{
			this.currentWorkspaceFile = realFilename;
		}
		else
		{
			this.currentWorkspaceFile = null;
		}
	}

	private void resetWorkspace()
	{
		this.closeWorkspace(false);
	}

	private String getRealWorkspaceFilename(String filename)
	{
		if (filename == null) return filename;
		filename = FileDialogUtil.replaceConfigDir(filename);

		WbFile wfile = new WbFile(filename);
		if (!wfile.isAbsolute())
		{
			wfile = new WbFile(Settings.getInstance().getConfigDir(), filename);
			filename = wfile.getFullPath();
		}
		return filename;
	}

	public boolean loadWorkspace(String filename, boolean updateRecent)
	{
		if (filename == null) return false;
		final String realFilename = getRealWorkspaceFilename(filename);

		WbFile f = new WbFile(realFilename);

	 	if (!f.exists())
		{
			// if the file does not exist, set all variables as if it did
			// thus the file will be created automatically.
			this.resetWorkspace();
			this.currentWorkspaceFile = realFilename;
			this.updateWindowTitle();
			this.checkWorkspaceActions();
			return true;
		}

		this.currentWorkspaceFile = null;
		this.resultForWorkspaceClose = false;

		WbSwingUtilities.invoke(new Runnable()
		{
			@Override
			public void run()
			{
				WbWorkspace w = null;
				try
				{
					removeAllPanels(false);

					// Ignore all stateChanged() events from the SQL Tab during loading
					tabRemovalInProgress = true;

					w = new WbWorkspace(realFilename, false);
					int entryCount = w.getEntryCount();
					for (int i = 0; i < entryCount; i++)
					{
						if (w.getPanelType(i) == PanelType.dbExplorer)
						{
							newDbExplorerPanel(false);
						}
						else
						{
							addTabAtIndex(false, false, false, -1);
						}
						MainPanel p = getSqlPanel(i);
						p.readFromWorkspace(w, i);
						((JComponent)p).invalidate();
					}

					currentWorkspaceFile = realFilename;
					resultForWorkspaceClose = true;

					renumberTabs();
					updateWindowTitle();
					checkWorkspaceActions();
					updateAddMacroAction();

					tabRemovalInProgress = false;

					int newIndex = w.getSelectedTab();
					if (newIndex < sqlTab.getTabCount())
					{
						sqlTab.setSelectedIndex(newIndex);
					}

					MainPanel p = getCurrentPanel();
					checkConnectionForPanel(p);
					setMacroMenuEnabled(true);
				}
				catch (Throwable e)
				{
					LogMgr.logWarning("MainWindow.loadWorkspace()", "Error loading workspace  " + realFilename, e);
					handleWorkspaceLoadError(e, realFilename);
					resultForWorkspaceClose = false;
				}
				finally
				{
					tabRemovalInProgress = false;
					FileUtil.closeQuietely(w);
					updateGuiForTab(sqlTab.getSelectedIndex());
				}
			}
		});

		if (updateRecent)
		{
			RecentWorkspaceManager.getInstance().workspaceLoaded(f);
			EventQueue.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					updateRecentWorkspaces();
				}
			});
		}

		return resultForWorkspaceClose;
	}

	private void loadWorkspaceForProfile(ConnectionProfile aProfile)
	{
		String realFilename = null;
		try
		{
			boolean useDefault = false;
			String workspaceFilename = aProfile.getWorkspaceFile();
			if (StringUtil.isBlank(workspaceFilename))
			{
				workspaceFilename = DEFAULT_WORKSPACE;
				useDefault = true;
			}
			else if (!workspaceFilename.endsWith(".wksp"))
			{
				workspaceFilename += ".wksp";
			}

			realFilename = getRealWorkspaceFilename(workspaceFilename);

			WbFile f = new WbFile(realFilename);

			if (realFilename.length() > 0 && !f.exists())
			{
				int action = useDefault ? CREATE_WORKSPACE : this.checkNonExistingWorkspace();
				if (action == LOAD_OTHER_WORKSPACE)
				{
					FileDialogUtil util = new FileDialogUtil();
					workspaceFilename = util.getWorkspaceFilename(this, false, true);
					aProfile.setWorkspaceFile(workspaceFilename);
				}
				else if (action == IGNORE_MISSING_WORKSPACE)
				{
					workspaceFilename = null;
					aProfile.setWorkspaceFile(null);
				}
				else
				{
					// start with an empty workspace
					// and create a new workspace file.
					resetWorkspace();
				}
			}

			if (StringUtil.isNonBlank(workspaceFilename))
			{
				// loadWorkspace will replace the %ConfigDir% placeholder,
				// so we need to pass the original filename
				this.loadWorkspace(workspaceFilename, false);
			}
		}
		catch (Throwable e)
		{
			LogMgr.logError("MainWindow.loadWorkspaceForProfile()", "Error reading workspace " + realFilename, e);
			this.handleWorkspaceLoadError(e, realFilename);
		}
	}

	public void forceDisconnect()
	{
		if (this.isConnectInProgress()) return;

		saveWorkspace(false);

		setConnectIsInProgress();
		showDisconnectInfo();
		try
		{
			final List<WbConnection> toAbort = new ArrayList<WbConnection>();

			for (int i=0; i < this.sqlTab.getTabCount(); i++)
			{
				final MainPanel sql = (MainPanel)this.sqlTab.getComponentAt(i);
				if (sql instanceof SqlPanel)
				{
					((SqlPanel)sql).forceAbort();
				}
				sql.disconnect();
				WbConnection con = sql.getConnection();
				if (con != null)
				{
					toAbort.add(con);
				}
				for (ToolWindow w : explorerWindows)
				{
					WbConnection conn = w.getConnection();
					if (conn != this.currentConnection)
					{
						toAbort.add(conn);
					}
				}
			}
			closeExplorerWindows(false);
			WbThread abort = new WbThread("Abort connections")
			{
				@Override
				public void run()
				{
					ConnectionMgr.getInstance().abortAll(toAbort);
				}
			};
			abort.start();
		}
		finally
		{
			closeConnectingInfo();
			// this must be called on the AWT thread
			// and it must be called synchronously!
			WbSwingUtilities.invoke(new Runnable()
			{
				@Override
				public void run()
				{
					disconnected(true);
				}
			});
		}
	}

	public void disconnect(final boolean background, final boolean closeWorkspace, final boolean saveWorkspace)
	{
		if (this.isConnectInProgress()) return;

		setConnectIsInProgress();

		if (saveWorkspace) saveWorkspace(false);
		if (background) showDisconnectInfo();

		Runnable run = new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					doDisconnect();
					if (closeWorkspace) closeWorkspace(background);
					if (background) closeConnectingInfo();
				}
				finally
				{
					clearConnectIsInProgress();
				}
			}
		};

		if (background)
		{
			Thread t = new WbThread(run, "MainWindow Disconnect");
			t.start();
		}
		else
		{
			run.run();
		}
	}

	/**
	 *	This does the real disconnect action.
	 */
	protected void doDisconnect()
	{
		try
		{
			ConnectionMgr mgr = ConnectionMgr.getInstance();
			WbConnection conn = null;

			for (int i = 0; i < this.sqlTab.getTabCount(); i++)
			{
				final MainPanel sql = (MainPanel) this.sqlTab.getComponentAt(i);
				if (sql instanceof SqlPanel)
				{
					((SqlPanel) sql).abortExecution();
				}
				conn = sql.getConnection();
				sql.disconnect();
				if (conn != null && !conn.isClosed())
				{
					showStatusMessage(ResourceMgr.getString("MsgDisconnecting"));
					mgr.disconnect(conn);
				}
			}
			closeExplorerWindows(true);
		}
		finally
		{
			// this must be called on the AWT thread
			// and it must be called synchronously!
			WbSwingUtilities.invoke(new Runnable()
			{
				@Override
				public void run()
				{
					disconnected(false);
				}
			});
		}
	}

	protected void disconnected(boolean closeWorkspace)
	{
		this.currentProfile = null;
		this.currentConnection = null;
		if (closeWorkspace)
		{
			this.closeWorkspace(false);
		}
		this.setMacroMenuEnabled(false);
		getJobIndicator().allJobsEnded();
		this.updateWindowTitle();
		this.disconnectAction.setEnabled(false);
		ShowDbmsManualAction.getInstance().setDbms(null, -1, -1);
		this.createNewConnection.checkState();
		this.disconnectTab.checkState();
		this.dbExplorerAction.setEnabled(false);
		this.newDbExplorerPanel.setEnabled(false);
		this.newDbExplorerWindow.setEnabled(false);
		this.showStatusMessage("");
		for (int i=0; i < sqlTab.getTabCount(); i++)
		{
			sqlTab.setForegroundAt(i, null);
		}
	}

	public void abortAll()
	{
		try
		{
			for (int i=0; i < this.sqlTab.getTabCount(); i++)
			{
				MainPanel sql = (MainPanel)this.sqlTab.getComponentAt(i);
				if (sql instanceof SqlPanel)
				{
					SqlPanel sp = (SqlPanel)sql;
					sp.forceAbort();
				}
			}
		}
		catch (Exception e)
		{
			LogMgr.logWarning("MainWindow.abortAll()", "Error stopping execution",e);
		}
	}

	private void selectCurrentEditor()
	{
		MainPanel p = this.getCurrentPanel();
		if (p instanceof SqlPanel)
		{
			SqlPanel sql = (SqlPanel)p;
			sql.selectEditor();
		}
	}

	protected String getCurrentEditorFile()
	{
		String filename = null;

		MainPanel p  = this.getCurrentPanel();
		if (p instanceof SqlPanel)
		{
			SqlPanel sql = (SqlPanel)p;
			filename = sql.getCurrentFileName();
		}
		return filename;
	}

	protected synchronized RunningJobIndicator getJobIndicator()
	{
		if (this.jobIndicator == null)
		{
			this.jobIndicator = new RunningJobIndicator(this);
		}

		return this.jobIndicator;
	}

	protected void updateWindowTitle()
	{
		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				WindowTitleBuilder titleBuilder = new WindowTitleBuilder();
				String title = titleBuilder.getWindowTitle(currentProfile, currentWorkspaceFile, getCurrentEditorFile());
				setTitle(title);
				getJobIndicator().baseTitleChanged();
			}
		});
	}

	protected void closeConnectingInfo()
	{
		getSelector().closeConnectingInfo();
	}

	protected void showDisconnectInfo()
	{
		getSelector().showDisconnectInfo();
	}

	/**
	 * Display a little PopupWindow to tell the user that the
	 * workbench is currently connecting to the DB
	 */
	protected void showConnectingInfo()
	{
		getSelector().showConnectingInfo();
	}

	private void setConnection(WbConnection con)
	{
		int count = this.sqlTab.getTabCount();
		for (int i=0; i < count; i++)
		{
			MainPanel sql = (MainPanel)this.sqlTab.getComponentAt(i);
			sql.setConnection(con);
		}
		this.currentConnection = con;
		if (this.currentProfile == null) this.currentProfile = con.getProfile();
	}

	public void selectConnection()
	{
		selectConnection(false);
	}

	private boolean exitOnCancel = false;

	public void selectConnection(boolean exit)
	{
		this.exitOnCancel = exit;
		getSelector().selectConnection();
	}

	public JMenu getRecentWorkspaceMenu(int panelIndex)
	{
		JMenu main = this.getMenu(ResourceMgr.MNU_TXT_WORKSPACE, panelIndex);
		if (main == null) return null;
		int count = main.getItemCount();
		for (int i=0; i < count; i ++)
		{
			JMenuItem item = main.getItem(i);
			if (item == null) continue;
			if ("recent-workspace".equals(item.getName()))
			{
				return (JMenu)item;
			}
		}
		return null;
	}

	public JMenu getMacroMenu(int panelIndex)
	{
		JMenu menu = this.getMenu(ResourceMgr.MNU_TXT_MACRO, panelIndex);
		return menu;
	}

	public JMenu getViewMenu(int panelIndex)
	{
		return this.getMenu(ResourceMgr.MNU_TXT_VIEW, panelIndex);
	}

	public JMenu getMenu(String aName, int panelIndex)
	{
		if (panelIndex < 0 || panelIndex >= this.panelMenus.size()) return null;
		if (aName == null) return null;
		JMenuBar menubar = this.panelMenus.get(panelIndex);
		int count = menubar.getMenuCount();
		for (int k=0; k < count; k++)
		{
			JMenu item = menubar.getMenu(k);
			if (item == null) continue;
			if (aName.equals(item.getName())) return item;
		}
		return null;
	}

	protected void updateRecentWorkspaces()
	{
		for (int i=0; i < getTabCount(); i++)
		{
			JMenu menu = getRecentWorkspaceMenu(i);
			RecentWorkspaceManager.getInstance().populateMenu(menu, this);
		}
	}

	protected void updateViewMenu(int sqlTabIndex, String aName)
	{
		int panelCount = this.panelMenus.size();
		if (aName == null) aName = ResourceMgr.getDefaultTabLabel();
		for (int i=0; i < panelCount; i++)
		{
			JMenu view = this.getViewMenu(i);

			int count = view.getItemCount();
			for (int k=0; k < count; k++)
			{
				JMenuItem item = view.getItem(k);
				if (item == null) continue;
				Action ac = item.getAction();
				if (ac == null) continue;

				if (ac instanceof SelectTabAction)
				{
					SelectTabAction a = (SelectTabAction)ac;
					if (a.getIndex() == sqlTabIndex)
					{
						a.setMenuText(aName);
						break;
					}
				}
			}
			WbSwingUtilities.repaintNow(view);
		}
	}

	/**
	 *	Add the approriate menu item to select a given tab
	 *  to the View menu.
	 */
	public void addToViewMenu(SelectTabAction anAction)
	{
		int panelCount = this.panelMenus.size();
		int lastActionIndex = -1;

		SelectTabAction lastAction = null;

		for (int i=0; i < panelCount; i++)
		{
			JMenu view = this.getViewMenu(i);

			// insert the item at the correct index
			// (if it is a SelectTabAction)
			// otherwise insert it after the last SelectTabAction
			int count = view.getItemCount();
			int inserted = -1;
			for (int k=0; k < count; k++)
			{
				JMenuItem item = view.getItem(k);
				if (item == null) continue;
				Action ac = item.getAction();
				if (ac == null) continue;
				if (!(ac instanceof SelectTabAction))
				{
					break;
				}
				SelectTabAction a = (SelectTabAction)ac;
				lastAction = a;
				lastActionIndex = k;

				if (a.getIndex() > anAction.getIndex())
				{
					view.insert(anAction, k);
					inserted = k;
					break;
				}
			}

			if (inserted == -1)
			{
				if (lastActionIndex == -1)
				{
					// no index found which is greater or equal than the new one
					// so add it to the end
					if (!(view.getItem(count -1).getAction() instanceof SelectTabAction))
					view.addSeparator();

					view.add(anAction);
				}
				else if (lastAction != null && lastAction.getIndex() != anAction.getIndex())
				{
					// we found at least one SelectTabAction, so we'll
					// insert the new one right behind the last one.
					// (there might be other items in the view menu!)

					view.insert(anAction, lastActionIndex + 1);
				}
			}
			else
			{
				// renumber the shortcuts for the remaining actions
				int newIndex = anAction.getIndex() + 1;
				for (int k=inserted + 1; k < panelCount; k++)
				{
					SelectTabAction a = (SelectTabAction)view.getItem(k).getAction();
					a.setNewIndex(newIndex);
					newIndex ++;
				}
			}
		}
	}

	private WbConnection getConnectionForTab(MainPanel aPanel, boolean returnNew)
		throws Exception
	{
		if (this.currentConnection != null && !returnNew) return this.currentConnection;
		String id = this.getConnectionIdForPanel(aPanel);

		aPanel.showStatusMessage(ResourceMgr.getString("MsgConnectingTo") + " " + this.currentProfile.getName() + " ...");
		ConnectionMgr mgr = ConnectionMgr.getInstance();
		WbConnection conn = null;
		try
		{
			conn = mgr.getConnection(this.currentProfile, id);
		}
		finally
		{
			aPanel.clearStatusMessage();
		}
		showConnectionWarnings(conn, aPanel);
		return conn;
	}

	private void showConnectionWarnings(WbConnection conn, MainPanel aPanel)
	{
		String warn = (conn != null ? conn.getWarnings() : null);
		if (warn != null)
		{
			aPanel.showResultPanel();
			aPanel.showLogMessage(ResourceMgr.getString("MsgConnectMsg") + "\n");
			aPanel.appendToLog(warn);
		}
	}

	public void addDbExplorerTab(DbExplorerPanel explorer)
	{
		JMenuBar dbmenu = this.createMenuForPanel(explorer);

		this.sqlTab.add(explorer);

		explorer.setTabTitle(this.sqlTab, this.sqlTab.getTabCount() - 1);

		SelectTabAction action = new SelectTabAction(this.sqlTab, this.sqlTab.getTabCount() - 1);
		action.setMenuText(explorer.getTabTitle());
		this.panelMenus.add(dbmenu);
		this.addToViewMenu(action);
	}

	public List<ToolWindow> getExplorerWindows()
	{
		return Collections.unmodifiableList(explorerWindows);
	}

	public void closeExplorerWindows(boolean doDisconnect)
	{
		for (ToolWindow w : explorerWindows)
		{
			WbConnection conn = w.getConnection();
			if (doDisconnect && conn != this.currentConnection)
			{
				ConnectionMgr.getInstance().disconnect(conn);
			}
			w.closeWindow();
		}
	}

	public void closeOtherPanels(MainPanel toKeep)
	{
		if (GuiSettings.getConfirmTabClose())
		{
			boolean doClose = WbSwingUtilities.getYesNo(sqlTab, ResourceMgr.getString("MsgConfirmCloseOtherTabs"));
			if (!doClose) return;
		}

		boolean inProgress = connectInProgress;
		if (!inProgress) this.setConnectIsInProgress();

		try
		{
			this.tabRemovalInProgress = true;
			int index = 0;
			while (index < sqlTab.getTabCount())
			{
				MainPanel p = getSqlPanel(index);

				if (p != toKeep && !p.isLocked())
				{
					if (p.isModified())
					{
						// if the panel is modified the user will be asked
						// if the panel should really be closed, in that
						// case I think it makes sense to make that panel the current panel
						selectTab(index);
						// tabSelected will not be run because tabRemovalInProgress == true
						tabSelected(index);
					}
					if (p.canClosePanel())
					{
						removeTab(index, false);
					}
					else
					{
						// if canCloseTab() returned false, then the user
						// selected "Cancel" which means stop closing panels
						// if the user selected "No" canCloseTab() will return "true"
						// to indicate whatever is in progress can go on.
						break;
					}
				}
				else
				{
					index ++;
				}
			}
			renumberTabs();
			// make sure the toolbar and menus are updated correctly
			updateCurrentTab(getCurrentPanelIndex());
			tabHistory.clear();
		}
		catch (Exception e)
		{
			LogMgr.logError("MainWindow.removeAllPanels()", "Error when removing all panels", e);
		}
		finally
		{
			tabRemovalInProgress = false;
			if (!inProgress) clearConnectIsInProgress();
		}
	}

	protected void removeAllPanels(boolean keepOne)
	{
		boolean inProgress = connectInProgress;
		if (!inProgress) this.setConnectIsInProgress();
		try
		{
			this.tabRemovalInProgress = true;
			int keep = (keepOne ? 1 : 0);
			while (sqlTab.getTabCount() > keep)
			{
				// I'm not using removeCurrentTab() as that will also
				// update the GUI and immediately check for a new
				// connection which is not necessary when removing all tabs.
				removeTab(keep, false);
			}
			// Reset the first panel, now we have a "clean" workspace
			if (keepOne)
			{
				MainPanel p = getSqlPanel(0);
				p.reset();
				resetTabTitles();

				// make sure the toolbar and menus are updated correctly
				updateCurrentTab(0);
			}
			tabHistory.clear();
		}
		catch (Exception e)
		{
			LogMgr.logError("MainWindow.removeAllPanels()", "Error when removing all panels", e);
		}
		finally
		{
			tabRemovalInProgress = false;
			if (!inProgress) clearConnectIsInProgress();
		}
	}

	/**
	 *	Returns the index of the first explorer tab
	 */
	public int findFirstExplorerTab()
	{
		int count = this.sqlTab.getTabCount();
		if (count <= 0) return -1;

		for (int i=0; i < count; i++)
		{
			Component c = this.sqlTab.getComponentAt(i);
			if (c instanceof DbExplorerPanel) return i;
		}
		return -1;
	}

	public void newDbExplorerWindow()
	{
		DbExplorerPanel explorer = new DbExplorerPanel(this);
		explorer.restoreSettings();
		DbExplorerWindow w = explorer.openWindow(this.currentProfile.getName());

		boolean useNewConnection = Settings.getInstance().getAlwaysUseSeparateConnForDbExpWindow()
			      || currentProfile.getUseSeparateConnectionPerTab()
						|| this.currentConnection == null;

		if (useNewConnection)
		{
			explorer.connect(this.currentProfile);
		}
		else
		{
			LogMgr.logDebug("MainWindow.newDbExplorerWindow()", "Re-using current connection for DbExplorer Window");
			explorer.setConnection(this.currentConnection);
		}
		explorerWindows.add(w);
	}

	public void explorerWindowClosed(DbExplorerWindow w)
	{
		if (isConnectInProgress()) return;
		explorerWindows.remove(w);
	}

	public void newDbExplorerPanel(boolean select)
	{
		DbExplorerPanel explorer = new DbExplorerPanel(this);
		explorer.restoreSettings();
		this.addDbExplorerTab(explorer);
		if (select)
		{
			// Switching to the new tab will initiate the connection if necessary
			this.sqlTab.setSelectedIndex(this.sqlTab.getTabCount() - 1);
		}
	}

	public ConnectionProfile getCurrentProfile()
	{
		return this.currentProfile;
	}

	public JMenu buildHelpMenu()
	{
		JMenu result = new WbMenu(ResourceMgr.getString(ResourceMgr.MNU_TXT_HELP));
		result.setName(ResourceMgr.MNU_TXT_HELP);
		new ShowHelpAction().addToMenu(result);
		new ShowManualAction().addToMenu(result);
		result.add(ShowDbmsManualAction.getInstance());
		result.add(HelpContactAction.getInstance());
		result.add(WhatsNewAction.getInstance());
		result.addSeparator();

		result.add(ViewLogfileAction.getInstance());
		new VersionCheckAction().addToMenu(result);
		new AboutAction().addToMenu(result);
		return result;
	}

	/**
	 * Create the tools menu for a panel menu. This will be called
	 * for each panel that gets added to the main window.
	 * Actions that are singletons (like the db explorer stuff)
	 * should not be created here
	 */
	public JMenu buildToolsMenu()
	{
		JMenu result = new WbMenu(ResourceMgr.getString(ResourceMgr.MNU_TXT_TOOLS));
		result.setName(ResourceMgr.MNU_TXT_TOOLS);

		result.add(this.dbExplorerAction);
		result.add(this.newDbExplorerPanel);
		result.add(this.newDbExplorerWindow);
		result.addSeparator();

		result.add(new DataPumperAction(this));
		result.add(new ObjectSearchAction(this));

		result.addSeparator();

		new OptionsDialogAction().addToMenu(result);
		new ConfigureShortcutsAction().addToMenu(result);

		return result;
	}

	private boolean checkMakeProfileWorkspace()
	{
		boolean assigned = false;
		boolean saveIt = WbSwingUtilities.getYesNo(this, ResourceMgr.getString("MsgAttachWorkspaceToProfile"));
		if (saveIt)
		{
			this.assignWorkspace();
			assigned = true;
		}
		return assigned;
	}

	/**
	 *	Sets the default title for all tab titles
	 */
	private void resetTabTitles()
	{
		String defaultTitle = ResourceMgr.getDefaultTabLabel();
		int count = this.sqlTab.getTabCount();
		for (int i=0; i < count; i++)
		{
			MainPanel p = this.getSqlPanel(i);
			if (p == null) continue;
			if (p instanceof SqlPanel)
			{
				SqlPanel sql = (SqlPanel)p;
				sql.closeFile(true, false);
				this.setTabTitle(i, defaultTitle);
			}
		}
	}

	public boolean isCancelling()
	{
		int count = this.sqlTab.getTabCount();
		for (int i=0; i < count; i++)
		{
			MainPanel p = this.getSqlPanel(i);
			if (p.isCancelling()) return true;
		}
		return false;

	}

	public boolean isConnected()
	{
		if (this.currentConnection != null)
		{
			return true;
		}
		int count = this.sqlTab.getTabCount();
		for (int i=0; i < count; i++)
		{
			MainPanel p = this.getSqlPanel(i);
			if (p.isConnected()) return true;
		}
		return false;
	}

	/**
	 *	Returns true if at least one of the SQL panels is currently
	 *  executing a SQL statement.
	 *  This method calls isBusy() for each tab.
	 */
	public boolean isBusy()
	{
		int count = this.sqlTab.getTabCount();
		for (int i=0; i < count; i++)
		{
			MainPanel p = this.getSqlPanel(i);
			if (p.isBusy()) return true;
		}
		return false;
	}

	public String getCurrentWorkspaceFile()
	{
		return this.currentWorkspaceFile;
	}

	public void loadWorkspace()
	{
		this.saveWorkspace();
		FileDialogUtil dialog = new FileDialogUtil();
		String filename = dialog.getWorkspaceFilename(this, false, true);
		if (filename == null) return;
		if (this.loadWorkspace(filename, true) && Settings.getInstance().getBoolProperty("workbench.gui.workspace.load.askassign", true))
		{
			checkMakeProfileWorkspace();
		}
		WbSwingUtilities.repaintLater(this);
	}

	/**
	 *	Closes the current workspace.
	 *  The tab count is reset to 1, the SQL history for the tab will be emptied
	 *  and the workspace filename will be "forgotten".
	 */
	public void closeWorkspace(boolean checkUnsaved)
	{
		this.currentWorkspaceFile = null;

		if (checkUnsaved)
		{
			int count = this.sqlTab.getTabCount();
			for (int i=0; i < count; i++)
			{
				MainPanel p = getSqlPanel(i);
				if (!p.canClosePanel()) return;
			}
		}

		WbSwingUtilities.invoke(new Runnable()
		{
			@Override
			public void run()
			{
        try
        {
					removeAllPanels(true);
        }
        catch (Exception e)
        {
          LogMgr.logError("MainWindow.closeWorkspace()", "Error when resetting workspace", e);
        }
        updateWindowTitle();
        checkWorkspaceActions();
			}
		});
	}

	/**
	 *	This will assign the current workspace name to the current profile.
	 */
	public void assignWorkspace()
	{
		if (this.currentWorkspaceFile == null) return;
		if (this.currentProfile == null) return;
		FileDialogUtil util = new FileDialogUtil();
		String filename = util.removeConfigDir(this.currentWorkspaceFile);
		this.currentProfile.setWorkspaceFile(filename);

		// The MainWindow gets a copy of the profile managed by the ConnectionMgr
		// so we need to update that one as well.
		ConnectionProfile realProfile = ConnectionMgr.getInstance().getProfile(currentProfile.getKey());
		if (realProfile != null)
		{
			realProfile.setWorkspaceFile(filename);
		}
		this.updateWindowTitle();
	}

	public boolean saveWorkspace()
	{
		return saveWorkspace(true);
	}
	/**
	 *	Save the currently loaded workspace
	 */
	public boolean saveWorkspace(boolean checkUnsaved)
	{
		if (this.currentWorkspaceFile != null)
		{
			return this.saveWorkspace(this.currentWorkspaceFile, checkUnsaved);
		}
		return true;
	}

	/**
	 *	Saves the current SQL history to a workspace with the given filename
	 *  If filename == null, a SaveAs dialog will be displayed.
	 *
	 *  If the workspace is saved with a new name (filename == null) the user
	 *  will be asked if the workspace should be assigned to the current profile
	 */
	public boolean saveWorkspace(String filename, boolean checkUnsaved)
	{
		if (!WbManager.getInstance().getSettingsShouldBeSaved()) return true;
		WbWorkspace w = null;
		boolean interactive = false;

		if (filename == null)
		{
			interactive = true;
			FileDialogUtil util = new FileDialogUtil();
			filename = util.getWorkspaceFilename(this, true, true);
			if (filename == null) return true;
		}

		String realFilename = getRealWorkspaceFilename(filename);
		WbFile f = new WbFile(realFilename);

		if (Settings.getInstance().getCreateWorkspaceBackup())
		{
			int maxVersions = Settings.getInstance().getMaxWorkspaceBackup();
			String dir = Settings.getInstance().getWorkspaceBackupDir();
			String sep = Settings.getInstance().getFileVersionDelimiter();
			FileVersioner version = new FileVersioner(maxVersions, dir, sep);
			try
			{
				version.createBackup(f);
			}
			catch (IOException e)
			{
				LogMgr.logWarning("MainWindow.saveWorkspace()", "Error when creating backup file!", e);
			}
		}
		else if (WbManager.getInstance().outOfMemoryOcurred())
		{
			// sometimes when an OoM occurred, saving of the workspace
			// succeeds but the ZIP file is not written correctly.
			// This tries to prevent the old file from beeing overwritten, just in case...
			f.makeBackup();
		}

		try
		{
			int count = this.sqlTab.getTabCount();

			if (checkUnsaved)
			{
				for (int i=0; i < count; i++)
				{
					MainPanel p = (MainPanel)this.sqlTab.getComponentAt(i);
					if (!p.canClosePanel()) return false;
				}
			}
			w = new WbWorkspace(realFilename, true);
			int selected = this.sqlTab.getSelectedIndex();
			w.setSelectedTab(selected);
			w.setEntryCount(count);
			for (int i=0; i < count; i++)
			{
				MainPanel p = getSqlPanel(i);
				p.saveToWorkspace(w,i);
			}
		}
		catch (Throwable e)
		{
			LogMgr.logError("MainWindow.saveWorkspace()", "Error saving workspace: " + filename, e);
			WbSwingUtilities.showErrorMessage(this, ResourceMgr.getString("ErrSavingWorkspace") + "\n" + ExceptionUtil.getDisplay(e));
		}
		finally
		{
			FileUtil.closeQuietely(w);
		}

		this.currentWorkspaceFile = filename;

		if (interactive)
		{
			this.checkMakeProfileWorkspace();
		}
		this.updateWindowTitle();
		this.checkWorkspaceActions();
		return true;
	}

	/**
	 *	Invoked when the a different SQL panel has been selected.
	 *
	 *  This fires the tabSelected() method.
	 *  @param e  a ChangeEvent object
	 *
	 */
	@Override
	public void stateChanged(ChangeEvent e)
	{
		if (e.getSource() == this.sqlTab)
		{
			if (this.tabRemovalInProgress) return;
			int index = this.sqlTab.getSelectedIndex();
			this.tabSelected(index);
		}
	}

	public MainPanel insertTab()
	{
		return addTab(true, true, false, true);
	}

	public MainPanel addTab()
	{
		return this.addTab(true, true, true, true);
	}

	/**
	 *  @param selectNew if true the new tab is automatically selected
	 *  @param checkConnection if true, the panel will automatically be connected
	 *  this is important if a Profile is used where each panel gets its own
	 *  connection
	 */
	public MainPanel addTab(boolean selectNew, boolean checkConnection)
	{
		return addTab(selectNew, checkConnection, true, true);
	}

	/**
	 * @param selectNew if true the new tab is automatically selected
	 * @param checkConnection if true, the panel will automatically be connected
	 * this is important if a Profile is used where each panel gets its own
	 * connection
	 * @param append if true, the tab will be appended at the end (after all other tabs), if false will be
	 * inserted before the current tab.
	 * @param renumber should the tabs be renumbered after adding the new tab. If several tabs are added
	 * in a loop renumber is only necessary at the end
	 *
	 * @see #renumberTabs()
	 * @see #checkConnectionForPanel(workbench.interfaces.MainPanel)
	 */
	public MainPanel addTab(boolean selectNew, boolean checkConnection, boolean append, boolean renumber)
	{
		int index = -1;
		if (append)
		{
			index = findFirstExplorerTab();

			// If the DbExplorer has been moved to somewhere else, then
			// add the tab really at the end.
			// Only if the DbExplorer is the last tab, insert the new tab before it
			if (index < sqlTab.getTabCount() - 1)
			{
				index = -1;
			}
		}
		else
		{
			index = this.sqlTab.getSelectedIndex() + 1;
		}
		return addTabAtIndex(selectNew, checkConnection, renumber, index);
	}

	private MainPanel addTabAtIndex(boolean selectNew, boolean checkConnection, boolean renumber, int index)
	{
		if (index == -1) index = sqlTab.getTabCount();

		final SqlPanel sql = new SqlPanel(index+1);

		try
		{
			tabRemovalInProgress = true;

			sql.setConnectionClient(this);
			sql.addDbExecutionListener(this);
			this.sqlTab.add(sql, index);
			sql.setTabTitle(sqlTab, index);

			JMenuBar menuBar = this.createMenuForPanel(sql);
			this.panelMenus.add(index, menuBar);

			if (checkConnection) this.checkConnectionForPanel(sql);

			// setTabTitle needs to be called after adding the panel!
			// this will set the correct title with Mnemonics
			// this.setTabTitle(index, ResourceMgr.getDefaultTabLabel());

			this.setMacroMenuEnabled(sql.isConnected());

			if (renumber) this.renumberTabs();
		}
		finally
		{
			tabRemovalInProgress = false;
		}
		sql.initDivider(sqlTab.getHeight() - sqlTab.getTabHeight());

		if (selectNew)
		{
			// if no connection was created initially the switch to a new
			// panel will initiate the connection.
			this.sqlTab.setSelectedIndex(index);
		}

		if (sqlTab.getTabCount() > 0)
		{
			sqlTab.setCloseButtonEnabled(0, this.sqlTab.getTabCount() > 1);
		}

		return sql;
	}

	/**
	 * Returns the real title of a tab (without the index number or any formatting)
	 *
	 * @see MainPanel#getTabTitle()
	 */
	public String getTabTitle(int index)
	{
		MainPanel panel = getSqlPanel(index);
		return panel.getTabTitle();
	}

	/**
	 * Returns the title of the currently selected tab.
	 *
	 * @see #getTabTitle(int)
	 * @see MainPanel#getTabTitle()
	 */
	@Override
	public String getCurrentTabTitle()
	{
		int index = this.sqlTab.getSelectedIndex();
		return this.getTabTitle(index);
	}

	@Override
	public void setCurrentTabTitle(String newName)
	{
		int index = this.sqlTab.getSelectedIndex();

		if (newName != null)
		{
			this.setTabTitle(index, newName);
		}
	}
	/**
	 *	Sets the title of a tab and appends the index number to
	 *  the title, so that a shortcut Ctrl-n can be defined
	 */
	public void setTabTitle(int anIndex, String aName)
	{
		MainPanel p = this.getSqlPanel(anIndex);
		p.setTabName(aName);
		p.setTabTitle(this.sqlTab, anIndex);
		updateViewMenu(anIndex, p.getTabTitle());
	}

	public void removeLastTab(boolean includeExplorer)
	{
		int index = this.sqlTab.getTabCount() - 1;
		if (!includeExplorer)
		{
			while (this.getSqlPanel(index) instanceof DbExplorerPanel)
			{
				index --;
			}
		}
		this.tabCloseButtonClicked(index);
	}

	/**
	 * Checks if the current tab is locked, or if it is the
	 * last tab that is open.
	 * <br/>
	 * This does not check if the user actually wants to close
	 * the tab!
	 *
	 * @return boolean if the current tab could be closed
	 */
	public boolean canCloseTab()
	{
		int index = sqlTab.getSelectedIndex();
		return canCloseTab(index);
	}

	@Override
	public boolean canCloseTab(int index)
	{
		if (index < 0) return false;
		MainPanel panel = this.getSqlPanel(index);
		if (panel == null) return false;
		if (panel.isLocked()) return false;

		int numTabs = sqlTab.getTabCount();
		return numTabs > 1;
	}

	@Override
	public Component getComponent()
	{
		return this;
	}

	@Override
	public boolean canRenameTab()
	{
		return (this.currentWorkspaceFile != null);
	}

	/**
	 * Closes the currently active tab.
	 *
	 * @see #tabCloseButtonClicked(int)
	 */
	public void removeCurrentTab()
	{
		int index = this.sqlTab.getSelectedIndex();
		this.tabCloseButtonClicked(index);
	}

	private void renumberTabs()
	{
		int count = this.sqlTab.getTabCount();
		for (int i=0; i < count; i++)
		{
			MainPanel p = this.getSqlPanel(i);
			p.setTabTitle(sqlTab, i);
		}
		for (int panel=0; panel < count; panel++)
		{
			rebuildViewMenu(panel);
		}
	}

	/**
	 * Rebuild the part of the view menu that handles the
	 * selecting of tabs
	 */
	private void rebuildViewMenu(int panel)
	{
		JMenu menu = this.getViewMenu(panel);
		JMenuItem item = menu.getItem(0);
		while (item != null && (item.getAction() instanceof SelectTabAction))
		{
			menu.remove(0);
			item = menu.getItem(0);
		}
		int count = this.sqlTab.getTabCount();
		for (int i=0; i < count; i++)
		{
			SelectTabAction a = new SelectTabAction(sqlTab, i);
			a.setMenuText(getTabTitle(i));
			menu.insert(a, i);
		}
		if (this.sqlTab.getSelectedIndex() == panel)
		{
			WbSwingUtilities.repaintNow(menu);
		}
	}

	/**
	 * Moves the current sql tab to the left (i.e. index := index - 1)
	 * If index == 0 nothing happens
	 */
	public void moveTabLeft()
	{
		int index = this.getCurrentPanelIndex();
		if (index <= 0) return;
		moveTab(index, index - 1);
	}

	/**
	 * Moves the current sql tab to the right (i.e. index := index + 1)
	 * If oldIndex denotes the last SQL Tab, nothing happens
	 */
	public void moveTabRight()
	{
		int index = this.getCurrentPanelIndex();
		int lastIndex = sqlTab.getTabCount();
		if (index >= lastIndex) return;
		moveTab(index, index + 1);
	}

	@Override
	public void moveCancelled()
	{
		this.tabRemovalInProgress = false;
	}

	@Override
	public void endMove(int finalIndex)
	{
		this.tabRemovalInProgress = false;
		this.tabSelected(finalIndex);
	}

	@Override
	public boolean startMove(int index)
	{
		this.tabRemovalInProgress = true;
		return true;
	}

	@Override
	public boolean moveTab(int oldIndex, int newIndex)
	{
		MainPanel panel = this.getSqlPanel(oldIndex);

		JMenuBar oldMenu = this.panelMenus.get(oldIndex);
		this.sqlTab.remove(oldIndex);
		this.panelMenus.remove(oldIndex);
		this.panelMenus.add(newIndex, oldMenu);

		this.sqlTab.add((JComponent)panel, newIndex);
		this.sqlTab.setSelectedIndex(newIndex);

		renumberTabs();
		this.validate();
		return true;
	}

	/**
	 * Removes the tab at the given location. If the current profile
	 * uses a separate connection per tab, then a disconnect will be
	 * triggered as well. This disconnect will be started in a
	 * background thread.
	 * <br/>
	 * The user will not be
	 */
	@Override
	public void tabCloseButtonClicked(int index)
	{
		MainPanel panel = this.getSqlPanel(index);
		if (panel == null) return;
		if (!panel.canClosePanel()) return;

		if (GuiSettings.getConfirmTabClose())
		{
			boolean doClose = WbSwingUtilities.getYesNo(sqlTab, ResourceMgr.getString("MsgConfirmCloseTab"));
			if (!doClose) return;
		}

		if (GuiSettings.getUseLRUForTabs())
		{
			tabHistory.restoreLastTab();
		}
		removeTab(index, true);
	}

	/**
	 * Removes the indicated tab without checking for modified file etc.
	 * If the tab has a separate connection, the connection is closed (disconnected)
	 * as well.
	 * If a single connection for all tabs is used, the connection is <b>not</b> closed.
	 */
	protected void removeTab(int index, boolean updateGUI)
	{
		MainPanel panel = this.getSqlPanel(index);
		if (panel == null) return;

		int newTab = -1;

		boolean inProgress = this.isConnectInProgress();
		if (!inProgress) this.setConnectIsInProgress();

		try
		{
			this.tabRemovalInProgress = true;

			WbConnection conn = panel.getConnection();

			// this does not really close the connection
			// it simply tells the panel that it should
			// release anything attached to the connection!
			// the actual disconnect from the DB is done afterwards
			// through the ConnectionMgr
			panel.disconnect();
			try
			{
				panel.dispose();
			}
			catch (Throwable th)
			{
				LogMgr.logError("MainWindow.removeTab()", "Error when removing tab", th);
			}

			boolean doDisconnect = conn != null && this.currentProfile != null && this.currentProfile.getUseSeparateConnectionPerTab();

			if (doDisconnect)
			{
				showStatusMessage(ResourceMgr.getString("MsgDisconnecting"));
				ConnectionMgr.getInstance().disconnect(conn);
				showStatusMessage("");
			}
			this.panelMenus.remove(index);
			this.sqlTab.remove(index);

			if (updateGUI)
			{
				this.renumberTabs();
				newTab = this.sqlTab.getSelectedIndex();
			}
		}
		catch (Throwable e)
		{
			LogMgr.logError("MainWindows.removeTab()", "Error removing tab index=" + index,e);
		}
		finally
		{
			this.tabRemovalInProgress = false;
			if (!inProgress) this.clearConnectIsInProgress();
		}
		if (newTab >= 0 && updateGUI)
		{
			this.tabSelected(newTab);
		}

		if (sqlTab.getTabCount() > 0)
		{
			sqlTab.setCloseButtonEnabled(0, this.sqlTab.getTabCount() > 1);
		}
	}

	@Override
	public void mouseClicked(MouseEvent e)
	{
		if (e.getSource() == this.sqlTab)
		{
			Point p = e.getPoint();
			int index = sqlTab.indexAtLocation(p.x, p.y);

			if (e.getButton() == MouseEvent.BUTTON2)
			{
				if (this.canCloseTab())
				{
					this.removeCurrentTab();
				}
			}

			if (e.getButton() == MouseEvent.BUTTON3)
			{
				SqlTabPopup pop = new SqlTabPopup(this);
				pop.show(this.sqlTab,e.getX(),e.getY());
			}
			else if (index == -1 && e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2)
			{
				this.addTab();
			}
		}
	}

	@Override
	public void mouseEntered(MouseEvent e)
	{
	}

	@Override
	public void mouseExited(MouseEvent e)
	{
	}

	@Override
	public void mousePressed(MouseEvent e)
	{
	}

	@Override
	public void mouseReleased(MouseEvent e)
	{
	}

	@Override
	public void executionEnd(WbConnection conn, Object source)
	{
		getJobIndicator().jobEnded();
	}

	@Override
	public void executionStart(WbConnection conn, Object source)
	{
		if (Settings.getInstance().getAutoSaveWorkspace())
		{
			this.saveWorkspace(false);
		}
		getJobIndicator().jobStarted();
	}

	@Override
	public void dragEnter(java.awt.dnd.DropTargetDragEvent dropTargetDragEvent)
	{
		dropTargetDragEvent.acceptDrag(DnDConstants.ACTION_COPY);
	}

	@Override
	public void dragExit(java.awt.dnd.DropTargetEvent dropTargetEvent)
	{
	}

	@Override
	public void dragOver(java.awt.dnd.DropTargetDragEvent dropTargetDragEvent)
	{
	}

	@Override
	public void drop(java.awt.dnd.DropTargetDropEvent dropTargetDropEvent)
	{
		try
		{
			Transferable tr = dropTargetDropEvent.getTransferable();
			if (tr.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
			{
				dropTargetDropEvent.acceptDrop(DnDConstants.ACTION_COPY);
				List fileList = (List)tr.getTransferData(DataFlavor.javaFileListFlavor);
				if (fileList != null)
				{
					int files = fileList.size();
					for (int i=0; i < files; i++)
					{
						File file = (File)fileList.get(i);
						this.addTab(true, true, true, true);
						SqlPanel sql = this.getCurrentSqlPanel();
						sql.readFile(file.getAbsolutePath(), null);
					}
				}
			}
			else
			{
				dropTargetDropEvent.rejectDrop();
			}
		}
		catch (IOException io)
		{
			LogMgr.logError("MainWindow.drop()", "Error processing drop event", io);
			dropTargetDropEvent.rejectDrop();
		}
		catch (UnsupportedFlavorException ufe)
		{
			LogMgr.logError("MainWindow.drop()", "Error processing drop event", ufe);
			dropTargetDropEvent.rejectDrop();
		}
	}

	@Override
	public void dropActionChanged(java.awt.dnd.DropTargetDragEvent dropTargetDragEvent)
	{
	}

}