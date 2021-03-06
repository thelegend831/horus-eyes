package br.com.fujitec.simulagent.strategies;

import br.com.fujitec.location.geoengine.GeoPosition;
import br.com.fujitec.simulagent.factories.PathFactory;
import br.com.fujitec.simulagent.models.Device;

/**
 * @author tiagoportela <tiagoporteladesouza@gmail.com>
 *
 */
public class MobileMovement extends MovementStrategy {

    @Override
    public void move(Device device) {
        final int deviceCurrentTime = device.getCurrentTime();
        device.setCurrentTime(deviceCurrentTime % PathFactory.TICKS_DAY);
        
        final GeoPosition deviceCurrentPosition = (GeoPosition) device.getPath().getPositionAtTime(deviceCurrentTime); 
        device.setCurrentPosition(deviceCurrentPosition);
        
        device.increaseCurrentTime();
    }
}
