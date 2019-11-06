package org.opendatakit.aggregate.client.popups;

import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import org.opendatakit.aggregate.client.form.KmlGeoTraceNShapeOption;
import org.opendatakit.aggregate.client.form.KmlOptionSetting;
import org.opendatakit.aggregate.client.form.KmlSelection;
import org.opendatakit.aggregate.client.widgets.AggregateCheckBox;
import org.opendatakit.aggregate.client.widgets.KmlSettingListBox;


public class KmlGeoTraceNShapeSelectionRow extends FlexTable implements KmlSelectionGeneration {

  private static final String NAME_TOOLTIP = "Champ de formulaire à utiliser comme nom";
  private static final String INCLUDE_TOOLTIP = "S'il faut inclure dans KML";

  private static final String NAME_BALLOON = "Choisissez le champ de formulaire à utiliser comme nom d'objet géographique";
  private static final String INCLUDE_BALLON = "Lorsque coché, l'élément géographique sera inclus dans KML";

  private final String formId;

  private final AggregateCheckBox include;
  private final KmlOptionSetting geoNode;
  private final KmlSettingListBox nameDropDown;

  KmlGeoTraceNShapeSelectionRow(String formID, KmlGeoTraceNShapeOption gtsNode) {
    formId = formID;
    geoNode = gtsNode.getGeoElement();

    include = new AggregateCheckBox(null, false, INCLUDE_TOOLTIP, INCLUDE_BALLON);
    include.setValue(true);
    nameDropDown = new KmlSettingListBox(NAME_TOOLTIP, NAME_BALLOON);
    nameDropDown.updateValues(gtsNode.getNameNodes(), true);

    addStyleName("dataTable");
    setWidget(0, 0, include);
    if (gtsNode.getType() == KmlGeoTraceNShapeOption.GeoTraceNShapeType.GEOTRACE) {
      setWidget(0, 1, new HTML("<h2>Geotrace:<h2>"));
    } else {
      setWidget(0, 1, new HTML("<h2>Geoshape:<h2>"));
    }
    setWidget(0, 2, new HTML(new SafeHtmlBuilder().appendEscaped(geoNode.getDisplayName()).toSafeHtml()));
    setWidget(0, 3, new HTML("<h4>Nom:<h4>"));
    setWidget(0, 4, nameDropDown);
  }

  @Override
  public KmlSelection generateKmlSelection() {
    // if not checked do not generate the information
    if (!include.getValue())
      return null;

    String geoPointValue = geoNode.getElementKey();
    String nameValue = nameDropDown.getElementKey();

    KmlSelection settings = new KmlSelection(formId);
    settings.setGeoTraceNShapeSelections(geoPointValue, nameValue);
    return settings;
  }
}
