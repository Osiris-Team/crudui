package org.vaadin.crudui.crud.impl;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalDataProvider;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalQuery;

import org.vaadin.crudui.crud.AbstractCrud;
import org.vaadin.crudui.crud.CrudListener;
import org.vaadin.crudui.crud.CrudOperation;
import org.vaadin.crudui.crud.CrudOperationException;
import org.vaadin.crudui.form.CrudFormFactory;
import org.vaadin.crudui.form.impl.form.factory.DefaultCrudFormFactory;
import org.vaadin.crudui.layout.CrudLayout;
import org.vaadin.crudui.layout.impl.HorizontalSplitCrudLayout;

/**
 * @author Alejandro Duarte
 */
public abstract class AbstractGridCrud<T> extends AbstractCrud<T> {

	protected String rowCountCaption = "%d items(s) found";
	protected String savedMessage = "Item saved";
	protected String deletedMessage = "Item deleted";
	protected boolean showNotifications = true;

	protected Button findAllButton;
	protected Button addButton;
	protected Button updateButton;
	protected Button deleteButton;
	protected Grid<T> grid;

	private boolean clickRowToUpdate;

	AbstractGridCrud(Class<T> domainType) {
		this(domainType, new HorizontalSplitCrudLayout(), new DefaultCrudFormFactory<>(domainType), null);
	}

	AbstractGridCrud(Class<T> domainType, CrudLayout crudLayout) {
		this(domainType, crudLayout, new DefaultCrudFormFactory<>(domainType), null);
	}

	AbstractGridCrud(Class<T> domainType, CrudFormFactory<T> crudFormFactory) {
		this(domainType, new HorizontalSplitCrudLayout(), crudFormFactory, null);
	}

	AbstractGridCrud(Class<T> domainType, CrudListener<T> crudListener) {
		this(domainType, new HorizontalSplitCrudLayout(), new DefaultCrudFormFactory<>(domainType), crudListener);
	}

	AbstractGridCrud(Class<T> domainType, CrudLayout crudLayout, CrudListener<T> crudListener) {
		this(domainType, crudLayout, new DefaultCrudFormFactory<>(domainType), crudListener);
	}

	AbstractGridCrud(Class<T> domainType, CrudLayout crudLayout, CrudFormFactory<T> crudFormFactory) {
		this(domainType, crudLayout, crudFormFactory, null);
	}

	AbstractGridCrud(Class<T> domainType, CrudLayout crudLayout, CrudFormFactory<T> crudFormFactory,
			CrudListener<T> crudListener) {
		super(domainType, crudLayout, crudFormFactory, crudListener);
		initLayout();
	}

	protected void initLayout() {
		findAllButton = new Button(VaadinIcon.REFRESH.create(), e -> findAllButtonClicked());
		findAllButton.getElement().setAttribute("title", "Refresh list");
		findAllButton.setVisible(false);

		crudLayout.addToolbarComponent(findAllButton);

		addButton = new Button(VaadinIcon.PLUS.create(), e -> addButtonClicked());
		addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		addButton.getElement().setAttribute("title", "Add");
		crudLayout.addToolbarComponent(addButton);

		updateButton = new Button(VaadinIcon.PENCIL.create(), e -> updateButtonClicked());
		updateButton.getElement().setAttribute("title", "Update");
		crudLayout.addToolbarComponent(updateButton);

		deleteButton = new Button(VaadinIcon.TRASH.create(), e -> deleteButtonClicked());
		deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
		deleteButton.getElement().setAttribute("title", "Delete");
		crudLayout.addToolbarComponent(deleteButton);

		grid = createGrid();
		grid.addSelectionListener(e -> gridSelectionChanged());
		crudLayout.setMainComponent(grid);

		updateButtons();
	}

	protected abstract Grid<T> createGrid();

	public abstract void refreshGrid();

	@Override
	protected void onAttach(AttachEvent attachEvent) {
		super.onAttach(attachEvent);
		refreshGrid();
	}

	@Override
	public void setAddOperationVisible(boolean visible) {
		addButton.setVisible(visible);
	}

	@Override
	public void setUpdateOperationVisible(boolean visible) {
		updateButton.setVisible(visible);
	}

	@Override
	public void setDeleteOperationVisible(boolean visible) {
		deleteButton.setVisible(visible);
	}

	@Override
	public void setFindAllOperationVisible(boolean visible) {
		findAllButton.setVisible(visible);
	}

	public HasValue<?, String> addFilterProperty(String caption) {
		TextField field = new TextField();
		field.setPlaceholder(caption);
		field.setPrefixComponent(VaadinIcon.SEARCH.create());
		field.setClearButtonVisible(true);
		field.addValueChangeListener(e -> findAllButtonClicked());
		crudLayout.addFilterComponent(field);
		return field;
	}

	public void setClickRowToUpdate(boolean clickRowToUpdate) {
		this.clickRowToUpdate = clickRowToUpdate;
	}

	protected void updateButtons() {
		boolean rowSelected = !grid.asSingleSelect().isEmpty();
		updateButton.setEnabled(rowSelected);
		deleteButton.setEnabled(rowSelected);
	}

	protected void gridSelectionChanged() {
		updateButtons();
		T domainObject = grid.asSingleSelect().getValue();

		if (domainObject != null) {
			if (clickRowToUpdate) {
				updateButtonClicked();
			} else {
				Component form = crudFormFactory.buildNewForm(CrudOperation.READ, domainObject, true, null,
						event -> grid.asSingleSelect().clear());
				String caption = crudFormFactory.buildCaption(CrudOperation.READ, domainObject);
				crudLayout.showForm(CrudOperation.READ, form, caption);
			}
		} else {
			crudLayout.hideForm();
		}
	}

	protected void findAllButtonClicked() {
		grid.asSingleSelect().clear();
		refreshGrid();

		int count = countRows();
		showNotification(String.format(rowCountCaption, count));
	}

	private int countRows() {
		var query = new Query();
		var provider = grid.getDataProvider();
		if (HierarchicalDataProvider.class.isAssignableFrom(provider.getClass()))
			query = new HierarchicalQuery(null, null);

		return provider.size(query);
	}

	protected void addButtonClicked() {
		grid.asSingleSelect().clear();
		T domainObject = crudFormFactory.getNewInstanceSupplier().get();
		showForm(CrudOperation.ADD, domainObject, false, savedMessage, event -> {
			try {
				T addedObject = addOperation.perform(domainObject);
				refreshGrid();
				grid.asSingleSelect().setValue(addedObject);
				grid.deselect(addedObject);
				showNotification(savedMessage);
				if (!grid.getClass().isAssignableFrom(TreeGrid.class)) {
					grid.scrollToItem(addedObject);
				}
			} catch (IllegalArgumentException ignore) {
			}
		});
	}

	protected void updateButtonClicked() {
		T domainObject = grid.asSingleSelect().getValue();
		showForm(CrudOperation.UPDATE, domainObject, false, savedMessage, event -> {
			try {
				T updatedObject = updateOperation.perform(domainObject);
				grid.asSingleSelect().clear();
				refreshGrid();
				grid.asSingleSelect().setValue(updatedObject);
				grid.deselect(updatedObject);
				showNotification(savedMessage);
				if (!grid.getClass().isAssignableFrom(TreeGrid.class)) {
					grid.scrollToItem(updatedObject);
				}
			} catch (CrudOperationException e1) {
				showNotification(e1.getMessage());
				throw e1;
			}
		});
	}

	protected void deleteButtonClicked() {
		T domainObject = grid.asSingleSelect().getValue();
		showDeleteConfirmation(domainObject);
	}

	public void showDeleteConfirmation(T domainObject) {
		showForm(CrudOperation.DELETE, domainObject, true, deletedMessage, event -> {
			try {
				deleteOperation.perform(domainObject);
				refreshGrid();
				grid.asSingleSelect().clear();
				showNotification(deletedMessage);
				crudLayout.hideForm();
			} catch (CrudOperationException e1) {
				showNotification(e1.getMessage());
				refreshGrid();
			} catch (Exception e2) {
				refreshGrid();
				throw e2;
			}
		});
	}

	protected void showForm(CrudOperation operation, T domainObject, boolean readOnly, String successMessage,
			ComponentEventListener<ClickEvent<Button>> buttonClickListener) {
		Component form = crudFormFactory.buildNewForm(operation, domainObject, readOnly, cancelClickEvent -> {
			if (clickRowToUpdate || operation == CrudOperation.ADD) {
				grid.asSingleSelect().clear();
				crudLayout.hideForm();
			} else {
				T selected = grid.asSingleSelect().getValue();
				grid.asSingleSelect().clear();
				grid.select(selected);
			}
		}, operationPerformedClickEvent -> {
			buttonClickListener.onComponentEvent(operationPerformedClickEvent);
			if (!clickRowToUpdate) {
				crudLayout.hideForm();
			}
		});
		String caption = crudFormFactory.buildCaption(operation, domainObject);
		crudLayout.showForm(operation, form, caption);
	}

	public Grid<T> getGrid() {
		return grid;
	}

	public Button getFindAllButton() {
		return findAllButton;
	}

	public Button getAddButton() {
		return addButton;
	}

	public Button getUpdateButton() {
		return updateButton;
	}

	public Button getDeleteButton() {
		return deleteButton;
	}

	public void setRowCountCaption(String rowCountCaption) {
		this.rowCountCaption = rowCountCaption;
	}

	public void setSavedMessage(String savedMessage) {
		this.savedMessage = savedMessage;
	}

	public void setDeletedMessage(String deletedMessage) {
		this.deletedMessage = deletedMessage;
	}

	public void setShowNotifications(boolean showNotifications) {
		this.showNotifications = showNotifications;
	}

	public void showNotification(String text) {
		if (showNotifications) {
			Notification.show(text);
		}
	}

}
