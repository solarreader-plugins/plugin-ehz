/*
 * Copyright (c) 2024-2025 Stefan Toengi
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.schnippsche.solarreader.test;

import de.schnippsche.solarreader.backend.connection.usb.UsbConnection;
import java.io.IOException;
import java.net.ConnectException;
import org.tinylog.Logger;

public class EhzUsbConnection implements UsbConnection {

  private String result;
  private boolean open = false;

  public EhzUsbConnection() {
    result =
        "1B1B1B1B010101017607001B0F778AD3620062007263010176010107001B173BD8F109080C2AED2D4C633C010163B5A0007607001B0F778AD46200620072630701770109080C2AED2D4C633C070100620AFFFF72620165173B1C9E7A77078181C78203FF0101010104454D480177070100000009FF0101010109080C2AED2D4C633C0177070100010800FF6401018201621E52FF560013E1C9930177070100020800FF6401018201621E52FF56002EBF2B620177070100010801FF0101621E52FF560013E1C9930177070100020801FF0101621E52FF56002EBF2B620177070100010802FF0101621E52FF5600000000000177070100020802FF0101621E52FF5600000000000177070100100700FF0101621B52FF5500004FD20177078181C78205FF0172620165173B1C9E01018302848B491A20A2348A6B395D984B33867213DA7E05B910310C1E08FFDA836B1A36B665CF153CC35734A204833C5EBBBA6101010163E14D007607001B0F778AD762006200726302017101633216000000001B1B1B1B1A";
  }

  @Override
  public void connect() throws ConnectException {
    Logger.debug("openPort");
    if (open) {
      throw new ConnectException("Port already in use");
    }
    open = true;
  }

  @Override
  public int readByte() throws IOException {
    if (result != null && !result.isEmpty()) {
      String hex = result.substring(0, 2);
      result = result.substring(2);
      return Integer.parseInt(hex, 16) & 0xFF;
    }
    throw new IOException("No more result found");
  }

  @Override
  public int writeBytes(byte[] bytes) {
    return -1;
  }

  @Override
  public void disconnect() {
    Logger.debug("closePort");
    if (open) {
      open = false;
    } else throw new RuntimeException("closed port without open");
  }
}
