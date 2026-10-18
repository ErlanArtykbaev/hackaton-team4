import {
  ActionIcon,
  Autocomplete,
  Button,
  Checkbox,
  Chip,
  CloseButton,
  Modal,
  MultiSelect,
  NumberInput,
  Radio,
  SegmentedControl,
  Select,
  Switch,
  TextInput,
  Textarea,
  createTheme,
} from '@mantine/core'

/**
 * Staff screens run on tablets: controls default to finger-sized targets (about 48 px),
 * text one step larger than Mantine's desktop defaults. `touch.css` keeps a 44 px floor
 * for controls that ask for a smaller size.
 */
export const theme = createTheme({
  primaryColor: 'teal',
  defaultRadius: 'md',
  fontSizes: { xs: '13px', sm: '15px', md: '16px', lg: '18px', xl: '20px' },
  components: {
    Button: Button.extend({ defaultProps: { size: 'lg' } }),
    ActionIcon: ActionIcon.extend({ defaultProps: { size: 'xl' } }),
    CloseButton: CloseButton.extend({ defaultProps: { size: 'xl' } }),
    TextInput: TextInput.extend({ defaultProps: { size: 'lg' } }),
    Textarea: Textarea.extend({ defaultProps: { size: 'lg' } }),
    // Stepper arrows are too small for a finger; the tablet opens a numeric keypad instead.
    NumberInput: NumberInput.extend({
      defaultProps: { size: 'lg', hideControls: true, inputMode: 'numeric' },
    }),
    Select: Select.extend({ defaultProps: { size: 'lg' } }),
    MultiSelect: MultiSelect.extend({ defaultProps: { size: 'lg' } }),
    Autocomplete: Autocomplete.extend({ defaultProps: { size: 'lg' } }),
    SegmentedControl: SegmentedControl.extend({ defaultProps: { size: 'lg' } }),
    Chip: Chip.extend({ defaultProps: { size: 'lg' } }),
    Checkbox: Checkbox.extend({ defaultProps: { size: 'md' } }),
    Radio: Radio.extend({ defaultProps: { size: 'md' } }),
    Switch: Switch.extend({ defaultProps: { size: 'lg' } }),
    Modal: Modal.extend({ styles: { title: { fontSize: 'var(--mantine-font-size-xl)', fontWeight: 700 } } }),
  },
})
