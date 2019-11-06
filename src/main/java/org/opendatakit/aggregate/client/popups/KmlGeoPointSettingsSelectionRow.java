package org.opendatakit.aggregate.client.popups;

import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import org.opendatakit.aggregate.client.form.KmlGeopointOption;
import org.opendatakit.aggregate.client.form.KmlOptionSetting;
import org.opendatakit.aggregate.client.form.KmlSelection;
import org.opendatakit.aggregate.client.widgets.AggregateCheckBox;
import org.opendatakit.aggregate.client.widgets.KmlSettingListBox;

public class KmlGeoPointSettingsSelectionRow extends FlexTable implements KmlSelectionGeneration {

  private static final String BINARY_TOOLTIP = "Champ binaire à afficher";
  private static final String TITLE_TOOLTIP = "Champ à utiliser comme titre";
  private static final String INCLUDE_TOOLTIP = "S'il faut inclure dans KML";

  private static final String TITLE_BALLOON = "Choisissez le champ pour le titre.";
  private static final String BINARY_BALLOON = "Choisissez le champ binaire à afficher.";
  private static final String INCLUDE_BALLON = "Lorsque coché, l'élément géographique sera inclus dans KML";

  private final String formId;
  private final AggregateCheckBox include;

  private final KmlOptionSetting geoPoint;
  private final KmlSettingListBox titleFieldsDropDown;
  private final KmlSettingListBox binaryFieldsDropDown;

  KmlGeoPointSettingsSelectionRow(String formID, KmlGeopointOption geopointNode) {
    formId = formID;
    geoPoint = geopointNode.getGeoElement();

    include = new AggregateCheckBox(null, false, INCLUDE_TOOLTIP, INCLUDE_BALLON);
    include.setValue(true);

    titleFieldsDropDown = new KmlSettingListBox(TITLE_TOOLTIP, TITLE_BALLOON);
    binaryFieldsDropDown = new KmlSettingListBox(BINARY_TOOLTIP, BINARY_BALLOON);

    titleFieldsDropDown.updateValues(geopointNode.getTitleNodes(), true);
    binaryFieldsDropDown.updateValues(geopointNode.getBinaryNodes(), true);

    addStyleName("dataTable");
    setWidget(0, 0, include);
    setWidget(0, 1, new HTML("<h2>Geopoint:<h2>"));
    setWidget(0, 2, new HTML(new SafeHtmlBuilder().appendEscaped(geoPoint.getDisplayName()).toSafeHtml()));
    setWidget(0, 3, new HTML("<h4>Titre:<h4>"));
    setWidget(0, 4, titleFieldsDropDown);
    setWidget(0, 5, new HTML("<h4>Image:<h4>"));
    setWidget(0, 6, binaryFieldsDropDown);
  }

  @Override
  public KmlSelection generateKmlSelection() {
    // if not checked do not generate the information
    if (!include.getValue())
      return null;

    String geoPointValue = geoPoint.getElementKey();
    String titleValue = titleFieldsDropDown.getElementKey();
    String binaryValue = binaryFieldsDropDown.getElementKey();

    KmlSelection settings = new KmlSelection(formId);
    settings.setGeoPointSelections(geoPointValue, titleValue, binaryValue);
    return settings;
  }

}
