package org.csstudio.sds.components.epics;

import org.csstudio.sds.components.model.LabelModel;
import org.csstudio.sds.model.initializers.AbstractControlSystemSchema;
import org.csstudio.sds.model.initializers.AbstractWidgetModelInitializer;

/**
 * Initializes a rectangle with EPICS specific property values.
 * 
 * @author Stefan Hofer + Sven Wende
 * @version $Revision$
 * 
 */
public final class LabelInitializer extends
		AbstractWidgetModelInitializer {

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void initialize(final AbstractControlSystemSchema schema) {
		initializeStaticProperty(LabelModel.PROP_LABEL, "Label");
		initializeDynamicProperty(LabelModel.PROP_LABEL, "$record$.VAL");
	}

}
