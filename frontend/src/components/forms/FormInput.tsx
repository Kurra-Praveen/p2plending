import { Controller} from 'react-hook-form';
import { TextField } from '@mui/material';
import type { Control, FieldValues, Path } from 'react-hook-form';
import type { TextFieldProps } from '@mui/material/TextField';

type FormInputProps<T extends FieldValues> = Omit<TextFieldProps, 'name'> & {
  name: Path<T>;
  control: Control<T>;
};

function FormInput<T extends FieldValues>({ name, control, ...props }: FormInputProps<T>) {
  return (
    <Controller
      name={name}
      control={control}
      render={({ field, fieldState: { error } }) => (
        <TextField
          {...field}
          {...props}
          onChange={(e) => {
            if (props.type === 'number') {
              const val = e.target.value;
              field.onChange(val === '' ? '' : Number(val));
            } else {
              field.onChange(e);
            }
          }}
          error={!!error}
          helperText={error?.message || props.helperText}
        />
      )}
    />
  );
}

export default FormInput;
