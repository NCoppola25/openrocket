package info.openrocket.swing.gui.dialogs.motor.thrustcurve;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.swing.RowFilter;
import javax.swing.table.TableModel;

import info.openrocket.core.database.motor.ThrustCurveMotorSet;
import info.openrocket.core.motor.Manufacturer;
import info.openrocket.core.motor.MotorConfiguration;
import info.openrocket.core.motor.ThrustCurveMotor;
import info.openrocket.core.rocketcomponent.MotorMount;
import info.openrocket.core.util.AbstractChangeSource;
import info.openrocket.core.util.ChangeSource;
import info.openrocket.core.util.StateChangeListener;

////////  Row filters

/**
 * Abstract adapter class.
 */
public class MotorRowFilter extends RowFilter<TableModel, Integer> implements ChangeSource {

	// configuration data used in the filter process
	private final ThrustCurveMotorDatabaseModel model;
	private List<ThrustCurveMotor> usedMotors = new ArrayList<ThrustCurveMotor>();

	private final AbstractChangeSource changeSourceDelegate = new AbstractChangeSource();
	private final Object change = new Object();

	// things which can be changed to modify filter behavior

	// Limit motors based on length
	private double minimumLength = 0;
	private double maximumLength = Double.MAX_VALUE;

	// Limit motors based on diameter
	private Double minimumDiameter;
	private Double maximumDiameter;

	// Collection of strings which match text in the motor
	private List<String> searchTerms = Collections.<String> emptyList();

	// Boolean which hides motors in the usedMotors list
	private boolean hideUsedMotors = false;

	// List of manufacturers to exclude.
	private List<Manufacturer> excludedManufacturers = new ArrayList<Manufacturer>();

	// Impulse class filtering
	private ImpulseClass minimumImpulse;
	private ImpulseClass maximumImpulse;
	
	// Show only available motors
	private boolean hideUnavailable = false;


	public MotorRowFilter(ThrustCurveMotorDatabaseModel model) {
		super();
		this.model = model;
	}

	public void setMotorMount( MotorMount mount ) {
		if (mount != null) {
			Iterator<MotorConfiguration> iter = mount.getMotorIterator();
			while( iter.hasNext()){
				MotorConfiguration mi = iter.next();
				if( !mi.isEmpty()){
					this.usedMotors.add((ThrustCurveMotor) mi.getMotor());
				}
			}
		}
	}

	public void setSearchTerms(final List<String> searchTerms) {
		this.searchTerms = new ArrayList<String>();
		for (String s : searchTerms) {
			s = s.trim().toLowerCase(Locale.getDefault());
			if (s.length() > 0) {
				this.searchTerms.add(s);
			}
		}
	}

	public double getMinimumLength() {
		return minimumLength;
	}

	public void setMinimumLength(double minimumLength) {
		if ( this.minimumLength != minimumLength ) {
			this.minimumLength = minimumLength;
			fireChangeEvent(change);
		}
	}

	public double getMaximumLength() {
		return maximumLength;
	}

	public void setMaximumLength(double maximumLength) {
		if ( this.maximumLength != maximumLength ) {
			this.maximumLength = maximumLength;
			fireChangeEvent(change);
		}
	}

	Double getMinimumDiameter() {
		return minimumDiameter;
	}

	void setMinimumDiameter(Double minimumDiameter) {
		this.minimumDiameter = minimumDiameter;
	}

	Double getMaximumDiameter() {
		return maximumDiameter;
	}

	void setMaximumDiameter(Double maximumDiameter) {
		this.maximumDiameter = maximumDiameter;
	}

	void setHideUsedMotors(boolean hideUsedMotors) {
		this.hideUsedMotors = hideUsedMotors;
	}

	List<Manufacturer> getExcludedManufacturers() {
		return excludedManufacturers;
	}

	void setExcludedManufacturers(Collection<Manufacturer> excludedManufacturers) {
		this.excludedManufacturers.clear();
		this.excludedManufacturers.addAll(excludedManufacturers);
	}

	ImpulseClass getMinimumImpulse() {
		return minimumImpulse;
	}

	void setMinimumImpulse(ImpulseClass minimumImpulse) {
		this.minimumImpulse = minimumImpulse;
	}

	ImpulseClass getMaximumImpulse() {
		return maximumImpulse;
	}

	void setMaximumImpulse(ImpulseClass maximumImpulse) {
		this.maximumImpulse = maximumImpulse;
	}

	public boolean isHideUnavailable() {
		return hideUnavailable;
	}

	public void setHideUnavailable(boolean hideUnavailable) {
		this.hideUnavailable = hideUnavailable;
	}

	@Override
	public boolean include(RowFilter.Entry<? extends TableModel, ? extends Integer> entry) {
		int index = entry.getIdentifier();
		ThrustCurveMotorSet m = model.getMotorSet(index);
		return filterManufacturers(m) && filterUsed(m) && filterBySize(m) && filterByString(m) && filterByImpulseClass(m) && filterUnavailable(m);
	}

	private boolean filterManufacturers(ThrustCurveMotorSet m) {
		if (excludedManufacturers.contains(m.getManufacturer())) {
			return false;
		} else {
			return true;
		}
	}

	private boolean filterUsed(ThrustCurveMotorSet m) {
		if (!hideUsedMotors) {
			return true;
		}
		for (ThrustCurveMotor motor : usedMotors) {
			if (m.matches(motor)) {
				return false;
			}
		}
		return true;
	}

	private boolean filterBySize(ThrustCurveMotorSet m) {

		if ( minimumDiameter != null ) {
			if ( m.getDiameter() <= minimumDiameter - 0.0015 ) {
				return false;
			}
		}

		if ( maximumDiameter != null ) {
			if ( m.getDiameter() >= maximumDiameter + 0.0004 ) {
				return false;
			}
		}

		if ( m.getLength() > maximumLength ) {
			return false;
		}
		
		if ( m.getLength() < minimumLength ) {
			return false;
		}

		return true;
	}


	private boolean filterByString(ThrustCurveMotorSet m) {
		main: for (String s : searchTerms) {
			for (ThrustCurveMotorColumns col : ThrustCurveMotorColumns.values()) {
				String str = col.getValue(m).toString().toLowerCase(Locale.getDefault());
				if (str.contains(s)) {
					continue main;
				}
			}

			// Make sure that you can search on both the common name, and designation
			// Yes, there is some duplication here because the common name or designation is already checked in the previous loop
			// but it's not worth checking that...
			String common = m.getCommonName().toLowerCase(Locale.getDefault());
			String designation = m.getDesignation().toLowerCase(Locale.getDefault());
			if (common.contains(s) || designation.contains(s)) {
				continue main;
			}
			return false;
		}
	return true;
	}

	private boolean filterByImpulseClass(ThrustCurveMotorSet m) {
		if ( minimumImpulse != null ) {
			if( m.getTotalImpulse() <= minimumImpulse.getLow() ) {
				return false;
			}
		}

		if ( maximumImpulse != null ) {
			if( m.getTotalImpulse() > maximumImpulse.getHigh() ) {
				return false;
			}
		}

		return true;
	}

	private boolean filterUnavailable(ThrustCurveMotorSet m) {
		if (!hideUnavailable) {
			return true;
		}
		return m.isAvailable();
	}


	public final void addChangeListener(StateChangeListener listener) {
		changeSourceDelegate.addChangeListener(listener);
	}

	public final void removeChangeListener(StateChangeListener listener) {
		changeSourceDelegate.removeChangeListener(listener);
	}

	public void fireChangeEvent(Object source) {
		changeSourceDelegate.fireChangeEvent(source);
	}

}
