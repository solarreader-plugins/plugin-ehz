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
package de.schnippsche.solarreader.plugins.ehz;

import de.schnippsche.solarreader.backend.calculator.SimpleCalculator;
import de.schnippsche.solarreader.backend.connection.general.ConnectionFactory;
import de.schnippsche.solarreader.backend.connection.usb.UsbConnection;
import de.schnippsche.solarreader.backend.connection.usb.UsbConnectionFactory;
import de.schnippsche.solarreader.backend.field.PropertyField;
import de.schnippsche.solarreader.backend.frame.ObisFrame;
import de.schnippsche.solarreader.backend.protocol.KnownProtocol;
import de.schnippsche.solarreader.backend.protocol.SmlProtocol;
import de.schnippsche.solarreader.backend.provider.AbstractUsbProvider;
import de.schnippsche.solarreader.backend.provider.CommandProviderProperty;
import de.schnippsche.solarreader.backend.provider.ProviderProperty;
import de.schnippsche.solarreader.backend.provider.SupportedInterface;
import de.schnippsche.solarreader.backend.table.Table;
import de.schnippsche.solarreader.backend.util.SerialPortConfigurationBuilder;
import de.schnippsche.solarreader.backend.util.Setting;
import de.schnippsche.solarreader.backend.util.StringConverter;
import de.schnippsche.solarreader.backend.util.TimeEvent;
import de.schnippsche.solarreader.database.Activity;
import de.schnippsche.solarreader.frontend.ui.HtmlInputType;
import de.schnippsche.solarreader.frontend.ui.HtmlWidth;
import de.schnippsche.solarreader.frontend.ui.UIInputElementBuilder;
import de.schnippsche.solarreader.frontend.ui.UIList;
import de.schnippsche.solarreader.frontend.ui.UITextElementBuilder;
import de.schnippsche.solarreader.plugin.PluginMetadata;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.tinylog.Logger;

/**
 * Represents a provider for interacting with EHZ (Elektronischer Haushaltszähler) devices over USB.
 * This class extends {@link AbstractUsbProvider} and provides functionality for communicating with
 * EHZ devices via USB connections.
 *
 * <p>The EHZ is an electronic household meter typically used in Germany and other countries for
 * measuring electricity consumption. This class allows the communication with EHZ devices,
 * receiving data over USB, making it suitable for integration with energy monitoring systems.
 */
@PluginMetadata(
    name = "Ehz",
    version = "1.0.1",
    author = "Stefan Töngi",
    url = "https://github.com/solarreader-plugins/plugin-Ehz",
    svgImage = "ehz.svg",
    supportedInterfaces = {SupportedInterface.NAMED_USB, SupportedInterface.LISTED_USB},
    usedProtocol = KnownProtocol.SML,
    supports = "EHZ")
public class Ehz extends AbstractUsbProvider {
  private static final String DEVICE_NUMBER_ID = "0100000009ff";
  private static final String[] OBIS_KEYS = {
    "OBIS070",
    "OBIS1270",
    "OBIS1570",
    "OBIS180",
    "OBIS181",
    "OBIS182",
    "OBIS183",
    "OBIS280",
    "OBIS281",
    "OBIS282",
    "OBIS283",
    "OBIS3270",
    "OBIS470",
    "OBIS5170",
    "OBIS5270",
    "OBIS7170",
    "OBIS7270",
    "OBIS7670"
  };
  private SmlProtocol protocol;

  /**
   * Constructs a new instance of the {@link Ehz} class with the default USB connection factory.
   *
   * <p>This constructor uses the default {@link UsbConnectionFactory} to manage USB connections. It
   * logs the instantiation of the object.
   */
  public Ehz() {
    this(new UsbConnectionFactory());
  }

  /**
   * Constructs a new instance of the {@link Ehz} class with the default USB connection factory or a
   * specified connection factory for managing USB connections.
   *
   * <p>The first constructor initializes the connection factory using the default {@link
   * UsbConnectionFactory}. The second constructor allows the user to provide a custom {@link
   * ConnectionFactory} for managing USB connections to the EHZ device. It also logs the
   * instantiation of the object.
   *
   * @param connectionFactory the {@link ConnectionFactory} used to create and manage USB
   *     connections for interacting with the EHZ device. If {@code null}, a default {@link
   *     UsbConnectionFactory} will be used.
   */
  public Ehz(ConnectionFactory<UsbConnection> connectionFactory) {
    super(connectionFactory);
    Logger.debug("instantiate {}", this.getClass().getName());
  }

  /**
   * Returns the {@link ResourceBundle} for the plugin based on the specified {@link Locale}.
   *
   * <p>This implementation retrieves the resource bundle associated with the plugin, which provides
   * localized content for the given {@link Locale}. The returned {@link ResourceBundle} is
   * predefined and does not change based on the locale parameter in this implementation.
   *
   * @return the {@link ResourceBundle} associated with the plugin.
   */
  @Override
  public ResourceBundle getPluginResourceBundle() {
    return ResourceBundle.getBundle("ehz", locale);
  }

  @Override
  public Activity getDefaultActivity() {
    return new Activity(TimeEvent.TIME, 0, TimeEvent.TIME, 86399, 10, TimeUnit.SECONDS);
  }

  @Override
  public Optional<UIList> getProviderDialog() {
    UIList uiList = new UIList();
    uiList.addElement(
        new UITextElementBuilder().withLabel(resourceBundle.getString("ehz.title")).build());
    uiList.addElement(
        new UIInputElementBuilder()
            .withId("id-ehz-timeout")
            .withRequired(true)
            .withType(HtmlInputType.NUMBER)
            .withStep("any")
            .withColumnWidth(HtmlWidth.HALF)
            .withLabel(resourceBundle.getString("ehz.timeout.text"))
            .withName(Setting.READ_TIMEOUT_MILLISECONDS)
            .withPlaceholder(resourceBundle.getString("ehz.timeout.text"))
            .withTooltip(resourceBundle.getString("ehz.timeout.tooltip"))
            .withInvalidFeedback(resourceBundle.getString("ehz.timeout.error"))
            .build());

    return Optional.of(uiList);
  }

  @Override
  public Optional<List<ProviderProperty>> getSupportedProperties() {
    return getSupportedPropertiesFromFile("ehz_fields.json");
  }

  @Override
  public Optional<List<Table>> getDefaultTables() {
    return getDefaultTablesFromFile("ehz_tables.json");
  }

  @Override
  public Setting getDefaultProviderSetting() {
    return new SerialPortConfigurationBuilder()
        .withBaudrate(9600)
        .withReadTimeoutMilliseconds(8000)
        .build();
  }

  @Override
  public void configurationHasChanged() {
    super.configurationHasChanged();
    this.protocol = new SmlProtocol(providerData.getSetting().getReadTimeoutMilliseconds());
  }

  @Override
  public String testProviderConnection(Setting setting) throws IOException {
    try (UsbConnection testUsbConnection = connectionFactory.createConnection(setting)) {
      testUsbConnection.connect();
      SmlProtocol smlProtocol = new SmlProtocol(10000);
      smlProtocol.receiveData(testUsbConnection);
      return "";
    }
  }

  @Override
  public void doOnFirstRun() {
    doStandardFirstRun();
  }

  @Override
  public boolean doActivityWork(Map<String, Object> variables) throws IOException {
    try (UsbConnection connection = getConnection()) {
      connection.connect();
      String receivedHexData = protocol.receiveData(connection);
      for (ProviderProperty property : providerData.getProviderProperties()) {
        if (property.isPreConditionTrue(variables)) {
          handleProperty(property, receivedHexData, variables);
        }
      }
      // Remove null values
      for (String key : OBIS_KEYS) {
        variables.putIfAbsent(key, BigDecimal.ZERO);
      }
      variables.putIfAbsent("Kennung", "");
      variables.putIfAbsent("Zaehlernummer", "");
      return true;
    }
  }

  private void handleProperty(
      ProviderProperty providerProperty, String received, Map<String, Object> variables) {
    switch (providerProperty.getProviderPropertyType()) {
      case COMMAND:
        CommandProviderProperty commandProviderProperty =
            (CommandProviderProperty) providerProperty;
        testObisFrame(commandProviderProperty, received, variables);
        break;
      case SIMPLE:
        new SimpleCalculator().calculate(providerProperty.getPropertyFieldList(), variables);
        break;
      default:
        throw new UnsupportedOperationException(
            "Unsupported property type: " + providerProperty.getProviderPropertyType());
    }
  }

  private void testObisFrame(
      CommandProviderProperty commandProviderProperty,
      String receivedHexData,
      Map<String, Object> variables) {
    for (PropertyField propertyField : commandProviderProperty.getPropertyFieldList()) {
      String index = propertyField.getIndex();
      String name = new StringConverter(propertyField.getFieldName()).normalized();
      ObisFrame obisFrame = new ObisFrame(index);
      if (obisFrame.search(receivedHexData)) {
        Object obisValue = getObisValue(obisFrame, index);
        propertyField.setReadValue(obisValue);
        propertyField.calculateWithReadValue(variables);
        variables.put(name, propertyField.getCalculated());
        Logger.debug("name: {}, value:{}", name, propertyField.getCalculated());
      } else {
        variables.put(name, null);
      }
    }
  }

  private Object getObisValue(ObisFrame obisFrame, String command) {
    Object obisValue = obisFrame.getValue();
    if (DEVICE_NUMBER_ID.equalsIgnoreCase(command) && obisValue instanceof String) {
      String stringValue = (String) obisValue;
      if (stringValue.length() == 8) {
        byte[] bytes = stringValue.getBytes(StandardCharsets.ISO_8859_1);
        long left = ((bytes[1] & 0xFFL) << 16) | ((bytes[2] & 0xFFL) << 8) | (bytes[3] & 0xFFL);
        char mid = (char) bytes[4];
        long right = ((bytes[5] & 0xFFL) << 16) | ((bytes[6] & 0xFFL) << 8) | (bytes[7] & 0xFFL);
        obisValue = String.format("%d%c%d", left, mid, right);
      }
    }
    return obisValue;
  }
}
