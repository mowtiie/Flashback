package com.mowtiie.flashback.ui.settings;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import com.google.android.material.color.DynamicColors;
import com.mowtiie.flashback.theme.ThemeController;
import com.mowtiie.flashback.theme.ThemePreferences;
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

import com.mowtiie.flashback.MainActivity;
import com.mowtiie.flashback.ui.AppBarLift;
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

        setupAppearanceControls();

        if (requireActivity() instanceof MainActivity) {
            AppBarLift.attach(((MainActivity) requireActivity()).getAppBar(), binding.settingsScroll);
        }
        observe();
    }

    /**
     * Theme mode and dynamic colour both change the palette, which requires the
     * activity to be recreated to re-inflate with the new theme. Contrast is an
     * overlay applied at activity creation, so it recreates too.
     */
    private void setupAppearanceControls() {
        ThemePreferences prefs = viewModel.themePrefs();

        checkMode(prefs.getThemeMode());
        binding.themeModeGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            int mode = checkedId == R.id.modeLight ? ThemePreferences.MODE_LIGHT
                    : checkedId == R.id.modeDark ? ThemePreferences.MODE_DARK
                    : ThemePreferences.MODE_SYSTEM;
            if (mode != prefs.getThemeMode()) {
                prefs.setThemeMode(mode);
                ThemeController.applyThemeMode(prefs);
                // AppCompat recreates for night-mode changes automatically.
            }
        });

        // Dynamic colour only means anything on 12+; hide the row otherwise.
        boolean dynamicAvailable = DynamicColors.isDynamicColorAvailable();
        binding.dynamicColorSwitch.setEnabled(dynamicAvailable);
        binding.dynamicColorSwitch.setChecked(dynamicAvailable && prefs.isDynamicColor());
        binding.dynamicColorSwitch.setOnCheckedChangeListener((button, checked) -> {
            prefs.setDynamicColor(checked);
            refreshContrastEnabled(prefs);
            requireActivity().recreate();
        });

        checkContrast(prefs.getContrast());
        binding.contrastGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            int contrast = checkedId == R.id.contrastMedium ? ThemePreferences.CONTRAST_MEDIUM
                    : checkedId == R.id.contrastHigh ? ThemePreferences.CONTRAST_HIGH
                    : ThemePreferences.CONTRAST_STANDARD;
            if (contrast != prefs.getContrast()) {
                prefs.setContrast(contrast);
                requireActivity().recreate();
            }
        });

        refreshContrastEnabled(prefs);
    }

    private void checkMode(int mode) {
        int id = mode == ThemePreferences.MODE_LIGHT ? R.id.modeLight
                : mode == ThemePreferences.MODE_DARK ? R.id.modeDark : R.id.modeSystem;
        binding.themeModeGroup.check(id);
    }

    private void checkContrast(int contrast) {
        int id = contrast == ThemePreferences.CONTRAST_MEDIUM ? R.id.contrastMedium
                : contrast == ThemePreferences.CONTRAST_HIGH ? R.id.contrastHigh
                : R.id.contrastStandard;
        binding.contrastGroup.check(id);
    }

    /**
     * Contrast is meaningless while dynamic colour is on (dynamic colour brings
     * its own palette), so the picker is dimmed and a hint explains why.
     */
    private void refreshContrastEnabled(ThemePreferences prefs) {
        boolean dynamicOn = DynamicColors.isDynamicColorAvailable() && prefs.isDynamicColor();
        binding.contrastHint.setVisibility(dynamicOn ? View.VISIBLE : View.GONE);
        binding.contrastLabel.setAlpha(dynamicOn ? 0.5f : 1f);
        for (int i = 0; i < binding.contrastGroup.getChildCount(); i++) {
            binding.contrastGroup.getChildAt(i).setEnabled(!dynamicOn);
        }
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
