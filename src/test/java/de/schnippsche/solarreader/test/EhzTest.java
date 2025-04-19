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

import de.schnippsche.solarreader.backend.connection.general.ConnectionFactory;
import de.schnippsche.solarreader.backend.connection.usb.UsbConnection;
import de.schnippsche.solarreader.database.ProviderData;
import de.schnippsche.solarreader.plugins.ehz.Ehz;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EhzTest {
  @Test
  void test() throws Exception {
    GeneralTestHelper generalTestHelper = new GeneralTestHelper();
    ConnectionFactory<UsbConnection> testFactory = knownConfiguration -> new EhzUsbConnection();
    ProviderData providerData = new ProviderData();
    providerData.setName("Ehz Test");
    providerData.setPluginName("Ehz");
    Ehz provider = new Ehz(testFactory);
    providerData.setSetting(provider.getDefaultProviderSetting());
    provider.setProviderData(providerData);
    generalTestHelper.testProviderInterface(provider);
    Map<String, Object> variables = providerData.getResultVariables();
    assert new BigDecimal("2043.4").equals(variables.get("OBIS070"));
    assert new BigDecimal("33356430.7").equals(variables.get("OBIS180"));
    assert new BigDecimal("33356430.7").equals(variables.get("OBIS181"));
    assert new BigDecimal("0").equals(variables.get("OBIS182"));
    assert new BigDecimal("78428041.8").equals(variables.get("OBIS280"));
    assert new BigDecimal("78428041.8").equals(variables.get("OBIS281"));
    assert new BigDecimal("0").equals(variables.get("OBIS282"));
    assert "797421-5006140".equals(variables.get("Zaehlernummer"));
  }
}
