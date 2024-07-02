package org.vaadin.crudui.form.impl.form.factory;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasValueAndElement;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.vaadin.crudui.crud.CrudOperation;
import org.vaadin.crudui.form.AbstractAutoGeneratedCrudFormFactory;

import java.util.List;

/**
 * @author Alejandro Duarte
 */
public class DefaultCrudFormFactory<T> extends AbstractAutoGeneratedCrudFormFactory<T> {

	private FormLayout.ResponsiveStep[] responsiveSteps;

	public DefaultCrudFormFactory(Class<T> domainType) {
		this(domainType, (FormLayout.ResponsiveStep[]) null);
	}

	public DefaultCrudFormFactory(Class<T> domainType, FormLayout.ResponsiveStep... responsiveSteps) {
		super(domainType);
		if (responsiveSteps != null) {
			this.responsiveSteps = responsiveSteps;
		} else {
			this.responsiveSteps = new FormLayout.ResponsiveStep[] {
					new FormLayout.ResponsiveStep("0em", 1),
					new FormLayout.ResponsiveStep("25em", 2)
			};
		}
	}

	@Override
	public Component buildNewForm(CrudOperation operation, T domainObject, boolean readOnly,
			ComponentEventListener<ClickEvent<Button>> cancelButtonClickListener,
			ComponentEventListener<ClickEvent<Button>> operationButtonClickListener) {
		FormLayout formLayout = new FormLayout();
		formLayout.setSizeFull();
		formLayout.setResponsiveSteps(responsiveSteps);

		List<HasValueAndElement> fields = buildFields(operation, domainObject, readOnly);
		fields.stream()
				.forEach(field -> formLayout.getElement().appendChild(field.getElement()));

		Component footerLayout = buildFooter(operation, domainObject, cancelButtonClickListener,
				operationButtonClickListener);

		com.vaadin.flow.component.orderedlayout.VerticalLayout mainLayout = new VerticalLayout(formLayout,
				footerLayout);
		mainLayout.setFlexGrow(1, formLayout);
		mainLayout.setHorizontalComponentAlignment(Alignment.END, footerLayout);
		mainLayout.setMargin(false);
		mainLayout.setPadding(false);
		mainLayout.setSpacing(true);

		configureForm(formLayout, fields);

		return mainLayout;
	}

	protected void configureForm(FormLayout formLayout, List<HasValueAndElement> fields) {
		// No-op
	}

	@Override
	public String buildCaption(CrudOperation operation, T domainObject) {
		// If null, CrudLayout.showForm will build its own, for backward compatibility
		return null;
	}

}
