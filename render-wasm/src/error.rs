use thiserror::Error;

// This is not really dead code, #[wasm_error] macro replaces this by something else.
#[allow(dead_code)]
pub type Result<T> = std::result::Result<T, Error>;

#[derive(Error, Debug)]
pub enum Error {
    #[error("[Recoverable] {0}")]
    RecoverableError(anyhow::Error),
    #[error("[Critical] {0}")]
    CriticalError(anyhow::Error),
}

impl From<Error> for u8 {
    fn from(error: Error) -> Self {
        match error {
            Error::RecoverableError(_) => 0x01,
            Error::CriticalError(_) => 0x02,
        }
    }
}
