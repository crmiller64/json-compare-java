package me.calebmiller.web.views;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

import me.calebmiller.web.comparator.JsonObjectComparator;
import me.calebmiller.web.views.model.FieldComparison;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Route(value = "compare")
@RouteAlias(value = "")
@PageTitle("JSON Compare")
@CssImport("./themes/json-compare-java/views/json-compare-view.css")
@CssImport(value = "./themes/json-compare-java/components/vaadin-grid.css", themeFor = "vaadin-grid")
public class JsonCompareView extends HorizontalLayout {

	private final Logger logger = LoggerFactory.getLogger(JsonCompareView.class);

	private static final ObjectMapper objectMapper = new ObjectMapper();
	private static final transient JsonObjectComparator jsonObjectComparator = new JsonObjectComparator();

	private ObjectNode object1;
	private ObjectNode object2;

	private TextArea uiTextArea1;
	private TextArea uiTextArea2;
	private Button uiCompareButton;

	private Details inputArea;
	private Details outputArea;

	public JsonCompareView() {
		setId("json-compare-view");
		addClassName("json-compare-view");

		uiTextArea1 = new TextArea("JSON Object 1");
		uiTextArea2 = new TextArea("JSON Object 2");
		uiCompareButton = new Button("Compare");

		uiCompareButton.addClickListener(e-> {
			Notification.show("Comparing JSON objects...");
			compareObjects();
		});

		HorizontalLayout horizontalLayout = new HorizontalLayout();
		horizontalLayout.setPadding(true);
		// Adds and flex-grows both components
		horizontalLayout.addAndExpand(uiTextArea1, uiTextArea2);

		VerticalLayout verticalLayout = new VerticalLayout();
		verticalLayout.add(horizontalLayout, uiCompareButton);
		verticalLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, uiCompareButton);
		verticalLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

		inputArea = new Details("Input", verticalLayout);
		inputArea.setOpened(true);
		
		outputArea = new Details("Result", new Span());
		outputArea.setEnabled(false);
		outputArea.setOpened(false);

		add(inputArea, outputArea);
	}

	private void compareObjects() {
		getJsonObjects();
		if (object1 != null && object2 != null) {
			List<FieldComparison> fieldComparisons = jsonObjectComparator.compare(object1, object2);
			try {
				TreeGrid<FieldComparison> grid = new TreeGrid<>();
				grid.setItems(fieldComparisons, FieldComparison::getChildFields);
				grid.addHierarchyColumn(FieldComparison::getFieldName).setHeader("Field");
				grid.addColumn(FieldComparison::getField1Value).setHeader("Object 1");
				grid.addColumn(FieldComparison::getField2Value).setHeader("Object 2");

				outputArea.setContent(grid);
				outputArea.setEnabled(true);
				outputArea.setOpened(true);
			} catch (Exception e) {
				logger.error("error setting result to view: {}", e.getMessage());
				showDialog("Error setting result to view.");
			}
		}
	}

	private void getJsonObjects() {
		String jsonString1 = uiTextArea1.getValue();
		String jsonString2 = uiTextArea2.getValue();
		try {
			JsonNode jsonNode1 = objectMapper.readTree(jsonString1);
			JsonNode jsonNode2 = objectMapper.readTree(jsonString2);
			object1 = jsonNode1.isObject() ? (ObjectNode) jsonNode1 : JsonNodeFactory.instance.objectNode();
			object2 = jsonNode2.isObject() ? (ObjectNode) jsonNode2 : JsonNodeFactory.instance.objectNode();
		} catch (JsonProcessingException e) {
			logger.error("error parsing json into object: {}", e.getMessage());
			showDialog("Error parsing entered JSON into object.");
		}
	}

	private void showDialog(String message) {
		Dialog dialog = new Dialog();
		dialog.add(new Text(message));
		dialog.setCloseOnEsc(false);
		dialog.setCloseOnOutsideClick(false);

		Button confirmButton = new Button("OK", event -> dialog.close());
		dialog.add(new Div(confirmButton));

		dialog.open();
	}
}