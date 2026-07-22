package com.mowtiie.flashback.ui.settings;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateFormat;

import java.util.Calendar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.android.material.textfield.TextInputEditText;
import com.mowtiie.flashback.R;
import com.mowtiie.flashback.databinding.DialogImportPasteBinding;
import com.mowtiie.flashback.databinding.FragmentSettingsBinding;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private SettingsViewModel viewModel;

    /** Text staged for export, held while the create-document picker is open. */
    private String pendingExportContent;

    private ActivityResultLauncher<String> createDocument;
    private ActivityResultLauncher<String[]> openDocument;
    private ActivityResultLauncher<String> requestNotifications;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        registerLaunchers();

        binding.reminderSwitch.setOnClickListener(v -> onReminderToggled());
        binding.rowReminderTime.setOnClickListener(v -> showTimePicker());

        binding.rowExport.setOnClickListener(v -> viewModel.prepareExportAll());
        binding.rowImport.setOnClickListener(v -> chooseImportSource());

        observe();
    }

    /**
     * SAF launchers must be registered before the fragment reaches STARTED, so
     * they live here rather than behind a click. CreateDocument returns the Uri
     * the user chose to save to; OpenDocument the file they picked to read.
     */
    private void registerLaunchers() {
        createDocument = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("application/json"), uri -> {
                    if (uri != null && pendingExportContent != null) {
                        viewModel.writeExport(uri, pendingExportContent);
                    }
                    pendingExportContent = null;
                });

        openDocument = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(), uri -> {
                    if (uri != null) {
                        viewModel.previewImportFromUri(uri);
                    }
                });

        requestNotifications = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), granted -> {
                    if (granted) {
                        viewModel.setReminderEnabled(true);
                    } else {
                        // Reflect the real state: the switch cannot stay on if
                        // the system will drop every notification.
                        binding.reminderSwitch.setChecked(false);
                        Snackbar.make(binding.getRoot(),
                                R.string.reminder_permission_denied,
                                Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Enabling reminders needs the POST_NOTIFICATIONS permission on Android 13+.
     * Requesting it here, on an explicit opt-in, is the moment the user has
     * shown they want reminders — better than asking at first launch.
     */
    private void onReminderToggled() {
        boolean wantsOn = binding.reminderSwitch.isChecked();
        if (!wantsOn) {
            viewModel.setReminderEnabled(false);
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS);
        } else {
            viewModel.setReminderEnabled(true);
        }
    }

    private void showTimePicker() {
        int[] time = viewModel.getReminderTime().getValue();
        int hour = time == null ? 20 : time[0];
        int minute = time == null ? 0 : time[1];

        int clockFormat = DateFormat.is24HourFormat(requireContext())
                ? TimeFormat.CLOCK_24H : TimeFormat.CLOCK_12H;

        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(clockFormat)
                .setHour(hour)
                .setMinute(minute)
                .setTitleText(R.string.reminder_time_title)
                .build();

        picker.addOnPositiveButtonClickListener(v ->
                viewModel.setReminderTime(picker.getHour(), picker.getMinute()));
        picker.show(getChildFragmentManager(), "reminder_time");
    }

    private void renderReminderTime(int hour, int minute) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        binding.reminderTimeValue.setText(
                DateFormat.getTimeFormat(requireContext()).format(cal.getTime()));
    }

    private void chooseImportSource() {
        String[] options = {
                getString(R.string.import_source_file),
                getString(R.string.import_source_paste)
        };
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.import_source_title)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // A broad filter, because many exporters mislabel JSON
                        // as text/plain or octet-stream.
                        openDocument.launch(new String[]{
                                "application/json", "text/plain", "*/*"});
                    } else {
                        showPasteDialog();
                    }
                })
                .show();
    }

    private void showPasteDialog() {
        DialogImportPasteBinding dialogBinding =
                DialogImportPasteBinding.inflate(getLayoutInflater());
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.import_paste_title)
                .setView(dialogBinding.getRoot())
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.import_paste_action, (dialog, which) -> {
                    TextInputEditText field = dialogBinding.pasteField;
                    String text = field.getText() == null ? "" : field.getText().toString();
                    viewModel.previewImportFromText(text);
                })
                .show();
    }

    private void observe() {
        viewModel.getExportPayload().observe(getViewLifecycleOwner(), content -> {
            if (content == null) {
                return;
            }
            pendingExportContent = content;
            createDocument.launch(getString(R.string.export_all_filename));
            viewModel.clearExportPayload();
        });

        viewModel.getPendingImport().observe(getViewLifecycleOwner(), pending -> {
            if (pending != null) {
                showImportPreview(pending);
            }
        });

        viewModel.getEvent().observe(getViewLifecycleOwner(), event -> {
            if (event == null) {
                return;
            }
            Snackbar.make(binding.getRoot(), event.message,
                    event.success ? Snackbar.LENGTH_SHORT : Snackbar.LENGTH_LONG).show();
            viewModel.clearEvent();
        });

        viewModel.getWorking().observe(getViewLifecycleOwner(), working ->
                binding.progress.setVisibility(
                        Boolean.TRUE.equals(working) ? View.VISIBLE : View.GONE));

        viewModel.getReminderEnabled().observe(getViewLifecycleOwner(), enabled -> {
            boolean on = Boolean.TRUE.equals(enabled);
            binding.reminderSwitch.setChecked(on);
            // The time row is meaningless while reminders are off.
            binding.rowReminderTime.setEnabled(on);
            binding.rowReminderTime.setAlpha(on ? 1f : 0.5f);
        });

        viewModel.getReminderTime().observe(getViewLifecycleOwner(), time -> {
            if (time != null) {
                renderReminderTime(time[0], time[1]);
            }
        });
    }

    private void showImportPreview(SettingsViewModel.Pending pending) {
        String deckText = pending.counts.decks == 1
                ? getString(R.string.import_preview_decks_one, pending.counts.decks)
                : getString(R.string.import_preview_decks_other, pending.counts.decks);
        String cardText = pending.counts.notes == 1
                ? getString(R.string.import_preview_cards_one, pending.counts.notes)
                : getString(R.string.import_preview_cards_other, pending.counts.notes);
        String tagText = pending.counts.tags > 0
                ? getString(R.string.import_preview_tags_suffix, pending.counts.tags)
                : "";

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.import_preview_title)
                .setMessage(getString(R.string.import_preview_body, deckText, cardText, tagText))
                .setNegativeButton(R.string.action_cancel,
                        (dialog, which) -> viewModel.clearPendingImport())
                .setPositiveButton(R.string.import_confirm,
                        (dialog, which) -> viewModel.confirmImport())
                .setOnCancelListener(dialog -> viewModel.clearPendingImport())
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
