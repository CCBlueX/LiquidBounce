import { type Dispatch, type SetStateAction, useState } from "react";

/**
 * Hook to store state in local storage
 * @param key The key to store the state under 
 * @param defaultValue The default value to use if no value is stored
 * @returns A tuple containing the state and a function to set the state
 */
export function useStoredState<T>(
  key: string,
  defaultValue: T
): [T, Dispatch<SetStateAction<T>>] {
  const [state, setState] = useState<T>(() => {
    const storedState = localStorage.getItem(key);

    if (storedState) {
      return JSON.parse(storedState);
    }

    return defaultValue;
  });

  function setStoredState(newState: SetStateAction<T>) {
    localStorage.setItem(key, JSON.stringify(newState));
    setState(newState);
  }

  return [state, setStoredState];
}
