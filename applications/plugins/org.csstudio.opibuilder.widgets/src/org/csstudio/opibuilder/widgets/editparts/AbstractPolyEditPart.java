/* 
 * Copyright (c) 2006 Stiftung Deutsches Elektronen-Synchroton, 
 * Member of the Helmholtz Association, (DESY), HAMBURG, GERMANY.
 *
 * THIS SOFTWARE IS PROVIDED UNDER THIS LICENSE ON AN "../AS IS" BASIS. 
 * WITHOUT WARRANTY OF ANY KIND, EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED 
 * TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR PARTICULAR PURPOSE AND 
 * NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE 
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR 
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE. SHOULD THE SOFTWARE PROVE DEFECTIVE 
 * IN ANY RESPECT, THE USER ASSUMES THE COST OF ANY NECESSARY SERVICING, REPAIR OR 
 * CORRECTION. THIS DISCLAIMER OF WARRANTY CONSTITUTES AN ESSENTIAL PART OF THIS LICENSE. 
 * NO USE OF ANY SOFTWARE IS AUTHORIZED HEREUNDER EXCEPT UNDER THIS DISCLAIMER.
 * DESY HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, 
 * OR MODIFICATIONS.
 * THE FULL LICENSE SPECIFYING FOR THE SOFTWARE THE REDISTRIBUTION, MODIFICATION, 
 * USAGE AND OTHER RIGHTS AND OBLIGATIONS IS INCLUDED WITH THE DISTRIBUTION OF THIS 
 * PROJECT IN THE FILE LICENSE.HTML. IF THE LICENSE IS NOT INCLUDED YOU MAY FIND A COPY 
 * AT HTTP://WWW.DESY.DE/LEGAL/LICENSE.HTM
 */

package org.csstudio.opibuilder.widgets.editparts;

import org.csstudio.opibuilder.properties.IWidgetPropertyChangeHandler;
import org.csstudio.opibuilder.widgets.model.AbstractPolyModel;
import org.csstudio.opibuilder.widgets.util.RotationUtil;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Polyline;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.gef.EditPart;

/**
 * Abstract EditPart controller for the Polyline/polygon widget.
 * 
 * @author Sven Wende & Stefan Hofer (part of code is copied from SDS)
 * @author Xihui Chen
 * 
 */
public abstract class AbstractPolyEditPart extends AbstractShapeEditPart {
	
	
	@Override
	public AbstractPolyModel getWidgetModel() {
		return (AbstractPolyModel)getModel();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void registerPropertyChangeHandlers() {
		super.registerPropertyChangeHandlers();
				
		// points
		IWidgetPropertyChangeHandler pointsHandler = new IWidgetPropertyChangeHandler() {
			public boolean handleChange(final Object oldValue,
					final Object newValue,
					final IFigure refreshableFigure) {
				Polyline polyline = (Polyline) refreshableFigure;

				PointList points = (PointList) newValue;

				// deselect the widget (this refreshes the polypoint drag
				// handles)
				int selectionState = getSelected();
				setSelected(EditPart.SELECTED_NONE);

				polyline.setPoints(points);
				doRefreshVisuals(polyline);

				// restore the selection state
				setSelected(selectionState);
				
				return false;
			}
		};
		setPropertyChangeHandler(AbstractPolyModel.PROP_POINTS, pointsHandler);
		
		
		IWidgetPropertyChangeHandler rotationHandler = new IWidgetPropertyChangeHandler(){
			public boolean handleChange(Object oldValue, Object newValue,
					IFigure figure) {
				getWidgetModel().setPoints(
						RotationUtil.rotatePoints(getWidgetModel().getOriginalPoints().getCopy(), 
								(Double)newValue), false);
				return false;
			}
		};
		
		setPropertyChangeHandler(AbstractPolyModel.PROP_ROTATION, rotationHandler);
		
		
	}
	
	
}
