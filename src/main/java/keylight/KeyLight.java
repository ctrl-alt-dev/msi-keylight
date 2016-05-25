/*
 * KeyLight (C) 2016 E.Hooijmeijer, LGPL v3 licensed
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package keylight;

import java.nio.ByteBuffer;

import org.usb4java.Context;
import org.usb4java.Device;
import org.usb4java.DeviceDescriptor;
import org.usb4java.DeviceHandle;
import org.usb4java.DeviceList;
import org.usb4java.LibUsb;
import org.usb4java.LibUsbException;

/**
 * Allows you to change the keyboard lighting settings on MSI Steel series
 * keyboards with the 1770:FF00 USB HID on Ubuntu.
 */
public class KeyLight {

	public static enum Region {
		LEFT, MIDDLE, RIGHT;
	}

	public static enum Color {
		OFF, RED, ORANGE, YELLOW, GREEN, LIGHTBLUE, BLUE, PURPLE, WHITE;
	}

	public static enum Level {
		HIGH, MED, LOW, GLOW;
	}

	public static enum LedMode {
		/** constant */
		NORMAL,
		/** left brighter than right */
		GAMING,
		/** slowly on off */
		BREATHE,
		/** fade to black */
		DEMO,
		/** slowly on off per region */
		WAVE;
	}

	public static void main(String[] args) {
		if ((args.length != 1) || (args[0].length() != 7)) {
			System.out.println("Usage: java -jar keylight.jar MLCLCLC");
			System.out.println(" M: Ledmode : 0..4 (normal,gaming,breathe,demo,wave)");
			System.out.println(" L: Level :   0..3 (high,med,low,glow)");
			System.out.println(" C: Color :   0..8 (off,red,orange,yellow,green,light blue,blue,purple,white)");
			System.out.println("");
			System.out.println("Example: purple keyboard");
			System.out.println(" java -jar keylight.jar 0171717");
			System.exit(0);
		}
		int[] values = parse(args[0]);
		Context context = new Context();
		int result = LibUsb.init(context);
		if (result != LibUsb.SUCCESS) {
			throw new LibUsbException("Unable to initialize libusb.", result);
		}
		try {
			Device device = findDevice((short) 0x1770, (short) 0xFF00);
			setMode(device, LedMode.values()[values[0]]);
			setColor(device, Region.LEFT, Color.values()[values[2]], Level.values()[values[1]]);
			setColor(device, Region.MIDDLE, Color.values()[values[4]], Level.values()[values[3]]);
			setColor(device, Region.RIGHT, Color.values()[values[6]], Level.values()[values[5]]);
		} finally {
			LibUsb.exit(context);
		}
	}

	private static int[] parse(String string) {
		int[] results = new int[7];
		for (int t = 0; t < results.length; t++) {
			results[t] = Integer.parseInt(string.substring(t, t + 1), 16);
		}
		return results;
	}

	/**
	 * sets the led mode on the keyboard.
	 * 
	 * @param dev
	 *            the USB device.
	 * @param mode
	 *            the LedMode to set.
	 */
	private static void setMode(Device dev, LedMode mode) {
		ByteBuffer buffer = ByteBuffer.allocateDirect(8);
		buffer.put(new byte[] { 1, 2, 65, (byte) (mode.ordinal() + 1), 0, 0, 0, (byte) 236 });
		report(dev, (byte) 0, buffer);
	}

	/**
	 * sets the led color and brightness level in the specified region on the
	 * keyboard.
	 * 
	 * @param dev
	 *            the USB device.
	 * @param region
	 *            the region to set.
	 * @param color
	 *            the color to set.
	 * @param level
	 *            the level to set.
	 */
	private static void setColor(Device dev, Region region, Color color, Level level) {
		ByteBuffer buffer = ByteBuffer.allocateDirect(8);
		buffer.put(new byte[] { 1, 2, 66, (byte) (region.ordinal() + 1), (byte) color.ordinal(), (byte) level.ordinal(),
				0, (byte) 236 });
		report(dev, (byte) 0, buffer);
	}

	/**
	 * connects to the USB device and executes a HID report command.
	 * 
	 * @param device
	 *            the device.
	 * @param report
	 *            the report.
	 * @param buffer
	 *            the report bytes.
	 */
	private static void report(Device device, byte report, ByteBuffer buffer) {
		DeviceHandle handle = new DeviceHandle();
		int result = LibUsb.open(device, handle);
		if (result != LibUsb.SUCCESS) {
			throw new LibUsbException("Unable to open USB device", result);
		}
		try {
			int iface = 0;
			//
			boolean detach = true; // capabilities say no, but it works?!
			//
			if (detach) {
				result = LibUsb.detachKernelDriver(handle, iface);
				if (result != LibUsb.SUCCESS)
					throw new LibUsbException("Unable to detach kernel driver", result);
			}
			//
			result = LibUsb.claimInterface(handle, iface);
			if (result != LibUsb.SUCCESS) {
				throw new LibUsbException("Unable to claim interface", result);
			}
			try {
				int transfered = LibUsb.controlTransfer(handle,
						(byte) (LibUsb.REQUEST_TYPE_CLASS | LibUsb.RECIPIENT_INTERFACE | LibUsb.ENDPOINT_OUT),
						(byte) 0x09, (short) (3 << 8 | buffer.get(0)), (short) iface, buffer, 1000);
				if (transfered < 0) {
					throw new LibUsbException("Control transfer failed", transfered);
				}
				//
			} finally {
				LibUsb.releaseInterface(handle, iface);
				if (result != LibUsb.SUCCESS) {
					throw new LibUsbException("Unable to claim interface", result);
				}
				if (detach) {
					result = LibUsb.attachKernelDriver(handle, iface);
					if (result != LibUsb.SUCCESS) {
						throw new LibUsbException("Unable to re-attach kernel driver", result);
					}
				}
			}
		} finally {
			LibUsb.close(handle);
		}
	}

	/**
	 * finds the device or fails.
	 * 
	 * @param vendorId
	 *            the vendorId.
	 * @param productId
	 *            the productId.
	 * @return the device.
	 */
	public static Device findDevice(short vendorId, short productId) {
		DeviceList list = new DeviceList();
		int result = LibUsb.getDeviceList(null, list);
		if (result < 0) {
			throw new LibUsbException("Unable to get device list", result);
		}
		try {
			for (Device device : list) {
				DeviceDescriptor descriptor = new DeviceDescriptor();
				result = LibUsb.getDeviceDescriptor(device, descriptor);
				if (result != LibUsb.SUCCESS) {
					throw new LibUsbException("Unable to read device descriptor", result);
				}
				if (descriptor.idVendor() == vendorId && descriptor.idProduct() == productId) {
					return device;
				}
			}
		} finally {
			LibUsb.freeDeviceList(list, true);
		}

		throw new IllegalArgumentException("Device not found.");
	}

}
