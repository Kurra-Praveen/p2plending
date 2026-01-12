import { Controller } from 'react-hook-form';
import type { Control, FieldValues, Path } from 'react-hook-form';
import { TextField, MenuItem } from '@mui/material';
import type { TextFieldProps } from '@mui/material';

export interface SelectOption {
  label: string;
  value: string | number;
}

type FormSelectProps<T extends FieldValues> = Omit<TextFieldProps, 'name'> & {
  name: Path<T>;
  control: Control<T>;
  options: SelectOption[];
};

function FormSelect<T extends FieldValues>({ name, control, options, ...props }: FormSelectProps<T>) {
  return (
    <Controller
      name={name}
      control={control}
      render={({ field, fieldState: { error } }) => (
        <TextField
          {...field}
          {...props}
          select
          error={!!error}
          helperText={error?.message || props.helperText}
        >
          {options.map((option) => (
            <MenuItem key={option.value} value={option.value}>
              {option.label}
            </MenuItem>
          ))}
        </TextField>
      )}
    />
  );
}

export default FormSelect;
