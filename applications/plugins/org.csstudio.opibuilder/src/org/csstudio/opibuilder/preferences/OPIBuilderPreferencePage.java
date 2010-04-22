package org.csstudio.opibuilder.preferences;

import org.csstudio.opibuilder.OPIBuilderPlugin;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

/**The preference page for OPIBuilder
 * @author Xihui Chen
 *
 */
public class OPIBuilderPreferencePage extends FieldEditorPreferencePage
		implements IWorkbenchPreferencePage {

	// private static final String RESTART_MESSAGE = "Changes only takes effect after restart.";
	private static final String PREF_QUALIFIER_ID = OPIBuilderPlugin.PLUGIN_ID;
	
	public OPIBuilderPreferencePage() {
		super(FieldEditorPreferencePage.GRID);
		setPreferenceStore(new ScopedPreferenceStore(new InstanceScope(), PREF_QUALIFIER_ID));
		setMessage("OPI Builder Preferences");
		
		
	}

	@Override
	protected void createFieldEditors() {
		final Composite parent = getFieldEditorParent();
		
		StringTableFieldEditor macrosEditor = new StringTableFieldEditor(
				PreferencesHelper.RUN_MACROS, "Macros: " , parent, new String[]{"Name", "Value"}, 
				new boolean[]{true, true}, new MacroEditDialog(parent.getShell()), new int[]{120, 120});
		addField(macrosEditor);
		WorkspaceFileFieldEditor colorEditor = 
			new WorkspaceFileFieldEditor(PreferencesHelper.COLOR_FILE, 
					"color file: ", new String[]{"def"}, parent);//$NON-NLS-2$
		addField(colorEditor);  
		
		WorkspaceFileFieldEditor fontEditor =
			new WorkspaceFileFieldEditor(PreferencesHelper.FONT_FILE, 
				"font file: ", new String[]{"def"}, parent);//$NON-NLS-2$
		addField(fontEditor);
		
		IntegerFieldEditor guiRefreshCycleEditor = 
			new IntegerFieldEditor(PreferencesHelper.OPI_GUI_REFRESH_CYCLE,
					"OPI GUI Refresh Cycle (ms)", parent);
		guiRefreshCycleEditor.setValidRange(10, 5000);
		addField(guiRefreshCycleEditor);
		
		BooleanFieldEditor autoSaveEditor = 
			new BooleanFieldEditor(PreferencesHelper.AUTOSAVE, 
					"Automatically save file before running.", parent);
		addField(autoSaveEditor);		
		
		BooleanFieldEditor noEditModeEditor = 
			new BooleanFieldEditor(PreferencesHelper.NO_EDIT, 
					"No-Editing mode", parent);				
		addField(noEditModeEditor);
		
		BooleanFieldEditor advanceGraphicsEditor = 
			new BooleanFieldEditor(PreferencesHelper.DISABLE_ADVANCED_GRAPHICS, 
					"Disable Advanced Graphics", parent);				
		addField(advanceGraphicsEditor);
		
		StringFieldEditor topOPIsEditor = 
			new StringFieldEditor(PreferencesHelper.TOP_OPIS, "Top OPIs", parent);
		addField(topOPIsEditor);
	}

	public void init(IWorkbench workbench) {
		
	}
	
//	@Override
//	public void propertyChange(PropertyChangeEvent event) {
//		super.propertyChange(event);
//		Object  src = event.getSource();
//		if(src instanceof FieldEditor){
//			String prefName = ((FieldEditor)src).getPreferenceName();
//			if(prefName.equals(PreferencesHelper.NO_EDIT))
//				setMessage(RESTART_MESSAGE, WARNING);
//		}
//	}
}
